package com.walmart.service.function;

import com.walmart.service.LambdaConfigurationModule;
import com.walmart.service.models.*;
import com.walmart.service.util.DynamoDBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;

@RestController
@EnableWebMvc
public class DeleteFiles {

    private final Logger logger = LoggerFactory.getLogger(DeleteFiles.class);
    private final DynamoDbClient dynamoDbClient;
    private final S3Client s3Client;
    private final String tableName;
    private final String bucketName;

    @Autowired
    public DeleteFiles(final DynamoDbClient dynamoDbClient,
                       final S3Client s3Client,
                       final LambdaConfigurationModule configurationModule) {
        this.dynamoDbClient = dynamoDbClient;
        this.s3Client = s3Client;
        this.tableName = configurationModule.getTableName();
        this.bucketName = configurationModule.getBucketName();
    }

    /**
     * Deletes a file from S3 and DDB.
     *
     * @param userId   The owner of the file.
     * @param fileName The name of the file.
     * @param fileId   The unique ID of the file.
     */
    public void deleteFile(final String userId, final String fileName, final String fileId) {
        logger.info("Attempting to delete the file with fileId = {}", fileId);
        final DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(String.format("%s/%s", userId, fileName))
                .build();
        s3Client.deleteObject(deleteObjectRequest);
        final DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(Collections.singletonMap(TableAttributes.FILE_ID_KEY, AttributeValue.builder().s(fileId).build()))
                .build();
        dynamoDbClient.deleteItem(deleteItemRequest);
        logger.info("Successfully deleted the file with fileId = {} from storage.", fileId);
    }

    /**
     * The file name and user id should uniquely identify a file.
     *
     * @param fileName Name of the file we're looking for (e.g. image.png).
     * @param userId   User ID of the person that owns the file.
     * @param context  The file that's stored in S3 is written to here.
     */
    public File deleteFileByName(final String userId,
                                 final String fileName,
                                 final HttpServletResponse context) {
        logger.info("Attempting to retrieve the file with file name = {} and user ID = {}", fileName, userId);

        try {
            final File fileInfo = DynamoDBUtil.getFileFromDDB(userId, fileName, tableName, dynamoDbClient);
            if (fileInfo.getFileUUID() == null) {
                throw new NoSuchElementException();
            }
            logger.info("Was able to find the file with File ID = {}", fileInfo.getFileUUID());
            deleteFile(userId, fileName, fileInfo.getFileUUID());
            return fileInfo;
        } catch (final NoSuchElementException e) {
            logger.error("Unable to delete file with name = {} and user ID = {} since it doesn't exist", fileName, userId);
            context.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            throw e;
        } catch (final Exception e) {
            logger.error("Was unable to retrieve the file with file name = {} and user ID = {}", fileName, userId, e);
            context.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw e;
        }
    }

    /**
     * Deletes a file based on the file UUID.
     *
     * @param fileId  The file UUID we're looking to delete.
     * @param context Used to .
     */
    @RequestMapping(path = "/deleteFile/{userId}/{fileId}", method = RequestMethod.DELETE)
    public File deleteFileById(@PathVariable("userId") final String userId,
                               @PathVariable("fileId") final String fileId,
                               @Context HttpServletResponse context) {
        logger.info("Attempting to delete the file with file ID = {}", fileId);
        try {
            final File fileInfo = DynamoDBUtil.getFileFromDDB(fileId, tableName, dynamoDbClient);
            if (!fileInfo.getOwnerID().equals(userId)) {
                context.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                throw new RuntimeException(String.format("User does not have access to the file Id = %s", fileId));
            }
            logger.info("Was able to find the file with File ID = {}", fileInfo.getFileUUID());
            deleteFile(fileInfo.getOwnerID(), fileInfo.getFileName(), fileId);
            return fileInfo;
        } catch (final NoSuchKeyException e) {
            logger.error("Unable to delete file with ID = {} and user ID = {} since it doesn't exist", fileId, userId);
            context.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            throw e;
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            logger.error("Was unable to delete the file with file ID = {}", fileId, e);
            context.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw e;
        }
    }

    /**
     * Deletes multiple files using the specified file Ids and file name/user id pairs.
     *
     * @param userId             The userId of the user making this request.
     * @param deleteFilesRequest A class detailing which files should be deleted.
     * @param context            Context used to determine if any errors were thrown when deleting files.
     */
    @RequestMapping(path = "/deleteFiles/{userId}", method = RequestMethod.POST)
    public MultipleFilesResponse deleteMultipleFiles(@PathVariable("userId") final String userId,
                                                     @RequestBody final DeleteFilesRequest deleteFilesRequest,
                                                     @Context HttpServletResponse context) {
        logger.info("Attempting to delete the files = {}", deleteFilesRequest);
        final ArrayList<File> successfulDeletes = new ArrayList<>();
        final ArrayList<Pair> failedDeletes = new ArrayList<>();
        for (final String fileId : deleteFilesRequest.getFileIds()) {
            try {
                successfulDeletes.add(deleteFileById(userId, fileId, context));
            } catch (final Exception e) {
                failedDeletes.add(new Pair(fileId, context.getStatus()));
            }
        }
        for (final String fileName : deleteFilesRequest.getFileNames()) {
            try {
                successfulDeletes.add(deleteFileByName(userId, fileName, context));
            } catch (final Exception e) {
                failedDeletes.add(new Pair(fileName, context.getStatus()));
            }
        }
        context.setStatus(HttpServletResponse.SC_OK);
        return new MultipleFilesResponse(successfulDeletes, failedDeletes);
    }
}
