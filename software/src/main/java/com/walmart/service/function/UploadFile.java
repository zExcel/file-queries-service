package com.walmart.service.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.walmart.service.LambdaConfigurationModule;
import com.walmart.service.errors.ErrorCode;
import com.walmart.service.errors.ValidationException;
import com.walmart.service.models.FileType;
import com.walmart.service.models.Header;
import com.walmart.service.models.Table;
import com.walmart.service.util.RequestUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

@RestController
@EnableWebMvc
public class UploadFile extends GenericFunction<UploadFile> implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final LambdaConfigurationModule configurationModule;
    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;
    private final String[] requiredHeaders = {Header.FILE_NAME, Header.USER_ID};
    private static final Logger logger = LoggerFactory.getLogger(UploadFile.class);

    @Override
    public Class<UploadFile> getType() {
        return UploadFile.class;
    }

    public UploadFile() {
        super();
        this.configurationModule = applicationContext.getBean(LambdaConfigurationModule.class);
        this.s3Client = configurationModule.getS3Client();
        this.dynamoDbClient = configurationModule.getDynamoDbClient();
    }


    private ByteArrayOutputStream createOutputStream(final APIGatewayProxyRequestEvent event) throws ValidationException {
        String contentType = null;
        try {
            byte[] bI = Base64.decodeBase64(event.getBody().getBytes());
            Map<String, String> hps = event.getHeaders();
            if (hps != null) {
                contentType = hps.get(Header.CONTENT_TYPE);
            }

            String[] boundaryArray = contentType.split("=");
            byte[] boundary = boundaryArray[1].getBytes();
            ByteArrayInputStream content = new ByteArrayInputStream(bI);
            MultipartStream multipartStream = new MultipartStream(content, boundary, bI.length, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            boolean nextPart = multipartStream.skipPreamble();

            //Loop through each segment
            while (nextPart) {
                multipartStream.readBodyData(out);
                nextPart = multipartStream.readBoundary();
            }

            return out;
        } catch (final Exception e) {
            logger.info("ERROR: Failed to create the multipart stream.");
            logger.info(ExceptionUtils.getStackTrace(e));
            throw new ValidationException("Failed to create the multipart stream", e, ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public void uploadFileToS3(final ByteArrayOutputStream outputStream, final Map<String, String> headers) throws ValidationException {
        InputStream fis = new ByteArrayInputStream(outputStream.toByteArray());
        final RequestBody requestBody = RequestBody.fromInputStream(fis, outputStream.toByteArray().length);

        final String fileName = headers.get(Header.FILE_NAME);
        final String userId = headers.get(Header.USER_ID);
        final String fileType = RequestUtils.getFileType(fileName);
        final String mediaType = FileType.getMediaType(fileType);
        final String contentType = String.format("%s/%s", mediaType, fileType);
        final String key = String.format("%s/%s", userId, fileName);
        final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .contentLength((long) outputStream.toByteArray().length)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000")
                .bucket(configurationModule.getBucketName())
                .key(key)
                .build();

        s3Client.putObject(putObjectRequest, requestBody);
    }

    public void createDDBEntry(final String userId) {
        final Map<String, AttributeValue> keyValuePairs = new HashMap<>();
        keyValuePairs.put(Table.FILE_UUID, AttributeValue.builder()
                .s(UUID.randomUUID().toString())
                .build());
        keyValuePairs.put(Table.USER_ID, AttributeValue.builder()
                .s(userId)
                .build());
        keyValuePairs.put(Table.CREATION_DATE, AttributeValue.builder()
                .s(Instant.now().toString())
                .build());
        final PutItemRequest putItemRequest = PutItemRequest.builder()
                .item(keyValuePairs)
                .tableName(configurationModule.getTableName())
                .build();
        dynamoDbClient.putItem(putItemRequest);
    }

    @Override
    @RequestMapping(path = "/uploadFile", method = RequestMethod.POST)
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
        event.setHeaders(RequestUtils.transformEventHeaders(event));
        logger.debug("HEADERS = {}", event.getHeaders());
        final APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            final Map<String, String> headers = event.getHeaders();
            logger.info(String.valueOf(headers));
            RequestUtils.validateRequest(event, requiredHeaders);
            final ByteArrayOutputStream outputStream = this.createOutputStream(event);
            this.uploadFileToS3(outputStream, headers);
            this.createDDBEntry(headers.get(Header.USER_ID));

            //Construct a response
            response.setStatusCode(200);
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("Status", "File stored in S3, entry created in DDB");
            String responseBodyString = new Gson().toJson(responseBody);
            response.setBody(responseBodyString);
        } catch (final ValidationException e) {
            logger.error("ERROR: Failed to process the request.");
            logger.error(ExceptionUtils.getStackTrace(e));
            return response.withStatusCode(e.getErrorCodes().get(0).getStatusCode())
                    .withBody(e.toString());
        } catch (final Exception e) {
            logger.error("ERROR: Failed to process the request.");
            logger.error(ExceptionUtils.getStackTrace(e));
            return response.withStatusCode(500).withBody("Failed to process the request.");
        }

        return response;
    }
}
