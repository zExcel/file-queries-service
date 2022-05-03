package com.walmart.service.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.LambdaRuntime;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.walmart.service.LambdaConfigurationModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@ContextConfiguration(classes = LambdaConfigurationModule.class)
public class UploadFileTests {
    final String fileName = "testing.png";
    final String userId = "testing";
    final Map<String, String> headers = new HashMap<>();

    final String body = "testing";
    final APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
            .withBody(body);

    @MockBean
    private S3Client s3Client;
    @MockBean
    private DynamoDbClient dynamoDbClient;
    @Autowired
    private LambdaConfigurationModule configurationModule;

    final UploadFile uploadFileHandler = new UploadFile(configurationModule, s3Client, dynamoDbClient);
    final Context mockedContext = Mockito.mock(Context.class);
    final LambdaLogger lambdaLogger = LambdaRuntime.getLogger();

    @BeforeEach
    void initialize() {
        headers.put("file-name", fileName);
        headers.put("user-id", userId);
        requestEvent.setHeaders(headers);
    }

    @Test
    void handleRequestWorks() {
        Mockito.when(mockedContext.getLogger()).thenReturn(lambdaLogger);
//        uploadFileHandler.handleRequest(body, headers);
    }
}
