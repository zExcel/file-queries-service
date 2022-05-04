package com.walmart.service.function;

import com.google.gson.Gson;
import com.walmart.service.LambdaApplication;
import com.walmart.service.LambdaConfigurationModule;
import com.walmart.service.models.TableAttributes;
import org.junit.jupiter.api.TestInstance;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.model.Delete;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest(classes = LambdaApplication.class)
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = LambdaConfigurationModule.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractLambdaTest {

    final Logger logger = LoggerFactory.getLogger(AbstractLambdaTest.class);
    static final String GET_FILE_BY_NAME_FORMAT = "/getFile/%s/%s";
    static final String GET_FILE_BY_ID_FORMAT = "/getFile/%s";
    static final String LIST_FILES_FORMAT = "/listFiles/%s";
    static final String UPLOAD_FILES_FORMAT = "/uploadFile/%s";
    static final String DELETE_FILE_BY_ID_FORMAT = "/deleteFile/%s/%s";
    static final String DELETE_MULTIPLE_FILES_FORMAT = "/deleteFiles/%s";

    static final String TEST_USER_ID = "testing";

    static final String JPEG_CONTENT_TYPE = "image/jpeg";
    static final String JPEG_PAYLOAD_FILE_NAME = "payload.jpeg";
    static final String JPEG_PAYLOAD_PATH = String.format("payloads/%s", JPEG_PAYLOAD_FILE_NAME);
    final MockMultipartFile jpegPayloadFile;

    static final String PNG_CONTENT_TYPE = "image/png";
    static final String PNG_PAYLOAD_FILE_NAME = "payload.png";
    static final String PNG_PAYLOAD_PATH = String.format("payloads/%s", PNG_PAYLOAD_FILE_NAME);
    final MockMultipartFile pngPayloadFile;

    static final String PDF_CONTENT_TYPE = "application/pdf";
    static final String PDF_PAYLOAD_FILE_NAME = "payload.pdf";
    static final String PDF_PAYLOAD_PATH = String.format("payloads/%s", PDF_PAYLOAD_FILE_NAME);
    final MockMultipartFile pdfPayloadFile;

    private InputStream createStreamForResource(final String filePath) {
        return AbstractLambdaTest.class.getClassLoader().getResourceAsStream(filePath);
    }

    private MockMultipartFile createMultipartFile(final String fileName, final String contentType,
                                                  final String filePath) throws IOException {
        return new MockMultipartFile("data", fileName, contentType, createStreamForResource(filePath));
    }

    public AbstractLambdaTest() throws IOException {
        jpegPayloadFile = createMultipartFile(JPEG_PAYLOAD_FILE_NAME, JPEG_CONTENT_TYPE, JPEG_PAYLOAD_PATH);
        pngPayloadFile = createMultipartFile(PNG_PAYLOAD_FILE_NAME, PNG_CONTENT_TYPE, PNG_PAYLOAD_PATH);
        pdfPayloadFile = createMultipartFile(PDF_PAYLOAD_FILE_NAME, PDF_CONTENT_TYPE, PDF_PAYLOAD_PATH);
    }

    @Autowired
    MockMvc mockMvc;
    final Gson gson = new Gson();
    @Autowired
    DynamoDbClient dynamoDbClient;
    @Autowired
    S3Client s3Client;
    @Autowired
    LambdaConfigurationModule configurationModule;

    public void clearTableAndS3() {
        final ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
                .bucket(configurationModule.getBucketName())
                .build();
        final ListObjectsResponse listObjectsResponse = s3Client.listObjects(listObjectsRequest);

        final DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                .bucket(configurationModule.getBucketName())
                .delete(Delete.builder()
                                .objects(listObjectsResponse.contents().stream().map(S3Object::key)
                                                 .map(key -> ObjectIdentifier.builder().key(key).build())
                                                 .collect(Collectors.toList()))
                                .build())
                .build();

        if (deleteObjectsRequest.delete().objects().size() != 0) {
            s3Client.deleteObjects(deleteObjectsRequest);
        }

        final ScanRequest scanRequest = ScanRequest.builder()
                .tableName(configurationModule.getTableName())
                .projectionExpression(TableAttributes.FILE_ID_KEY)
                .build();
        final ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);

        final List<WriteRequest> writeRequests = scanResponse.items().stream().map(key -> WriteRequest.builder()
                .deleteRequest(DeleteRequest.builder()
                                       .key(key)
                                       .build())
                .build()).collect(Collectors.toList());

        final BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
                .requestItems(Collections.singletonMap(configurationModule.getTableName(), writeRequests))
                .build();

        if (writeRequests.size() != 0) {
            dynamoDbClient.batchWriteItem(batchWriteItemRequest);
        }
    }
}
