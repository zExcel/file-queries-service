package com.walmart.service.function;

import com.walmart.service.LambdaConfigurationModule;
import com.walmart.service.models.File;
import com.walmart.service.models.TableAttributes;
import com.walmart.service.util.DynamoDBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@EnableWebMvc
public class GetFile {

    private final Logger logger = LoggerFactory.getLogger(GetFile.class);
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Autowired
    public GetFile(final DynamoDbClient dynamoDbClient,
                   final LambdaConfigurationModule configurationModule) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = configurationModule.getTableName();
    }

    /**
     * The file name and user id should uniquely identify a file.
     * @param fileName  Name of the file we're looking for (e.g. image.png).
     * @param userId    User ID of the person that owns the file.
     * @return          The file that's stored in the database (or nothing if not found).
     */
    @RequestMapping(path = "/getFile/{userId}/{fileName}", method = RequestMethod.GET)
    public File retrieveFileByName(@PathVariable("userId") final String userId,
                                   @PathVariable("fileName") final String fileName) {
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
                return new File();
            }

            final Map<String, AttributeValue> item = queryResponse.items().get(0);
            final String fileId = item.get(TableAttributes.FILE_ID_KEY).s();
            logger.info("Was able to find the file with file name = {} and user ID = {}. File ID = {}", fileName, userId, fileId);

            return DynamoDBUtil.getFileFromDDB(fileId, tableName, dynamoDbClient);
        } catch (final Exception e) {
            logger.error("Was unable to retrieve the file with file name = {} and user ID = {}", fileName, userId, e);
            throw e;
        }
    }

    /**
     * Retrieves a file based on the file UUID.
     * @param fileId    The file UUID we're looking for in the DDB.
     * @return          The file, or none if not found.
     */
    @RequestMapping(path = "/getFile/{fileId}", method = RequestMethod.GET)
    public File retrieveFileById(@PathVariable("fileId") final String fileId) {
        logger.info("Attempting to retrieve the file with file ID = {}", fileId);
        try {
            return DynamoDBUtil.getFileFromDDB(fileId, tableName, dynamoDbClient);
        } catch (final Exception e) {
            logger.error("Was unable to retrieve the file with file ID = {}", fileId, e);
            throw e;
        }
    }
}
