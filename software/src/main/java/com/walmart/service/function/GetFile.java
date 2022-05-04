package com.walmart.service.function;

import com.walmart.service.LambdaConfigurationModule;
import com.walmart.service.models.File;
import com.walmart.service.models.Header;
import com.walmart.service.models.TableAttributes;
import com.walmart.service.util.DynamoDBUtil;
import com.walmart.service.util.S3Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@EnableWebMvc
public class GetFile {

    private final Logger logger = LoggerFactory.getLogger(GetFile.class);
    private final DynamoDbClient dynamoDbClient;
    private final S3Client s3Client;
    private final String tableName;
    private final String bucketName;

    @Autowired
    public GetFile(final DynamoDbClient dynamoDbClient,
                   final S3Client s3Client,
                   final LambdaConfigurationModule configurationModule) {
        this.dynamoDbClient = dynamoDbClient;
        this.s3Client = s3Client;
        this.tableName = configurationModule.getTableName();
        this.bucketName = configurationModule.getBucketName();
    }

    /**
     * The file name and user id should uniquely identify a file.
     * @param fileName  Name of the file we're looking for (e.g. image.png).
     * @param userId    User ID of the person that owns the file.
     * @param context   The file that's stored in S3 is written to here.
     */
    @RequestMapping(path = "/getFile/{userId}/{fileName}", method = RequestMethod.GET)
    public void retrieveFileByName(@PathVariable("userId") final String userId,
                                   @PathVariable("fileName") final String fileName,
                                   @Context final HttpServletResponse context) throws IOException {
        logger.info("Attempting to retrieve the file with file name = {} and user ID = {}", fileName, userId);

        try {
            final String keyExpression = String.format("%s = %s and %s = %s",
                                                       TableAttributes.FILE_NAME_KEY, ":name",
                                                       TableAttributes.USER_ID_KEY, ":user");
            final Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":name", AttributeValue.builder().s(fileName).build());
            expressionAttributeValues.put(":user", AttributeValue.builder().s(userId).build());

            final QueryRequest queryRequest = QueryRequest.builder()
                    .keyConditionExpression(keyExpression)
                    .expressionAttributeValues(expressionAttributeValues)
                    .indexName(TableAttributes.FILE_NAME_INDEX_KEY)
                    .tableName(tableName)
                    .build();
            final QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
            if (!queryResponse.hasItems() || queryResponse.items().isEmpty()) {
                logger.warn("Was unable to find any items with file name = {} and user ID = {}", fileName, userId);
                return;
            }

            final Map<String, AttributeValue> item = queryResponse.items().get(0);
            final String fileId = item.get(TableAttributes.FILE_ID_KEY).s();
            logger.info("Was able to find the file with file name = {} and user ID = {}. File ID = {}", fileName, userId, fileId);

            final ResponseInputStream<GetObjectResponse> response = S3Util.getS3File(fileName, userId, bucketName, s3Client);
            logger.debug("Found response from S3: {}", response);

            final int contentLength = Math.toIntExact(response.response().contentLength());
            context.setContentLength(contentLength);
            context.setContentType(response.response().contentType());
            context.setHeader(Header.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            final ServletOutputStream outputStream = context.getOutputStream();
            byte[] bbuf = new byte[contentLength + 1024];
            DataInputStream in = new DataInputStream(response);
            int length;
            while ((length = in.read(bbuf)) != -1) {
                outputStream.write(bbuf, 0, length);
            }
            in.close();
            outputStream.flush();
        } catch (final S3Exception e) {
            logger.error("Something went wrong when finding the file with file name = {} and user ID = {} in S3", fileName, userId, e);
            throw e;
        }
        catch (final Exception e) {
            logger.error("Was unable to retrieve the file with file name = {} and user ID = {}", fileName, userId, e);
            throw e;
        }
    }

    /**
     * Retrieves a file based on the file UUID.
     * @param fileId    The file UUID we're looking for in the DDB.
     * @param context   The file that's stored in S3 is written to here.
     */
    @RequestMapping(path = "/getFile/{fileId}", method = RequestMethod.GET)
    public void retrieveFileById(@PathVariable("fileId") final String fileId,
                                 @Context HttpServletResponse context) throws IOException {
        logger.info("Attempting to retrieve the file with file ID = {}", fileId);
        try {
            final File fileInfo = DynamoDBUtil.getFileFromDDB(fileId, tableName, dynamoDbClient);
            logger.debug("Found fileInfo = {} in the DDB", fileInfo);

            retrieveFileByName(fileInfo.getOwnerID(), fileInfo.getFileName(), context);
        } catch (final Exception e) {
            logger.error("Was unable to retrieve the file with file ID = {}", fileId, e);
            throw e;
        }
    }
}
