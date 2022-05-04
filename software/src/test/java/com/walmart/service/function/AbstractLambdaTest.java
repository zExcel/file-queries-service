package com.walmart.service.function;

import com.google.gson.Gson;
import com.walmart.service.LambdaApplication;
import com.walmart.service.LambdaConfigurationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.io.InputStream;

@SpringBootTest(classes = LambdaApplication.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = LambdaConfigurationModule.class)
public abstract class AbstractLambdaTest {
    static final Logger logger = LoggerFactory.getLogger(AbstractLambdaTest.class);

    static final String GET_FILE_BY_NAME_FORMAT = "/getFile/%s/%s";
    static final String GET_FILE_BY_ID_FORMAT = "/getFile/%s";
    static final String UPLOAD_FILES_FORMAT = "/uploadFile/%s";

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
    Gson gson = new Gson();
    @Autowired
    DynamoDbClient dynamoDbClient;
    @Autowired
    S3Client s3Client;
}
