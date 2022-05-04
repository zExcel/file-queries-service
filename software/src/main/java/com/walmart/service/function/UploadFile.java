package com.walmart.service.function;

import com.walmart.service.LambdaConfigurationModule;
import com.walmart.service.errors.ValidationException;
import com.walmart.service.models.File;
import com.walmart.service.models.FileType;
import com.walmart.service.models.MultipleFilesResponse;
import com.walmart.service.models.Pair;
import com.walmart.service.util.DynamoDBUtil;
import com.walmart.service.util.RequestUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

@RestController
@EnableWebMvc
public class UploadFile {

    private final LambdaConfigurationModule configurationModule;
    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;
    private static final Logger logger = LoggerFactory.getLogger(UploadFile.class);

    @Autowired
    public UploadFile(final LambdaConfigurationModule configurationModule,
                      final S3Client s3Client,
                      final DynamoDbClient dynamoDbClient) {
        this.configurationModule = configurationModule;
        this.s3Client = s3Client;
        this.dynamoDbClient = dynamoDbClient;
    }

    public void uploadFileToS3(final InputStream inputStream, final long fileSize,
                               final String fileName, final String userId) throws ValidationException {
        final software.amazon.awssdk.core.sync.RequestBody requestBody = software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, fileSize);

        final FileType fileType = RequestUtils.getFileType(fileName);
        final String mediaType = FileType.getMediaType(fileType);
        final String contentType = String.format("%s/%s", mediaType, fileType);
        final String key = String.format("%s/%s", userId, fileName);
        final PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .contentLength(fileSize)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000")
                .bucket(configurationModule.getBucketName())
                .key(key)
                .build();

        s3Client.putObject(putObjectRequest, requestBody);
        logger.info("Successfully put the file = {}/{} into S3", userId, fileName);
    }

    public String createDDBEntry(final String userId, final String fileName, final String creationDate) {
        final String fileId = UUID.randomUUID().toString();
        final Map<String, AttributeValue> keyValuePairs = DynamoDBUtil.createAttributeValueMap(fileId, userId, fileName, creationDate);

        final PutItemRequest putItemRequest = PutItemRequest.builder()
                .item(keyValuePairs)
                .tableName(configurationModule.getTableName())
                .build();
        dynamoDbClient.putItem(putItemRequest);
        logger.info("Successfully put the file = {}/{} as an entry in the database", userId, fileName);
        return fileId;
    }

    public File uploadFile(final MultipartFile data,
                           final String userId,
                           final String fileName) throws Exception {
        try {
            final String creationDate = Instant.now().toString();
            RequestUtils.validateFileName(fileName);
            this.uploadFileToS3(data.getInputStream(), data.getSize(), fileName, userId);
            final String fileId = this.createDDBEntry(userId, fileName, creationDate);

            final File response = new File(fileName, fileId, userId, creationDate);
            logger.info("File = {} successfully stored in S3 and DDB", response);
            return response;
        } catch (final Exception e) {
            logger.error("ERROR: Failed to process the request.");
            logger.error(ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    @PostMapping(path = "/uploadFile/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public MultipleFilesResponse uploadMultipleFiles(@RequestBody List<MultipartFile> data,
                                                     @PathVariable("userId") final String userId) {
        final ArrayList<File> fileResponses = new ArrayList<>();
        final ArrayList<Pair> failedFileNames = new ArrayList<>();
        for (final MultipartFile file : data) {
            String fileName = "Unknown";
            try {
                fileName = file.getOriginalFilename();
                fileResponses.add(uploadFile(file, userId, fileName));
            } catch (final Exception e) {
                logger.error("ERROR: Failed to process the file = {}", fileName);
                logger.error(ExceptionUtils.getStackTrace(e));
                failedFileNames.add(new Pair(fileName, HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
            }
        }
        return new MultipleFilesResponse(fileResponses, failedFileNames);
    }
}
