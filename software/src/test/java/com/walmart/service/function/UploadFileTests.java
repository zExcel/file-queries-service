package com.walmart.service.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.LambdaRuntime;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.walmart.service.errors.ValidationException;
import com.walmart.service.util.RequestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class UploadFileTests {
    final String fileName = "testing.png";
    final String userId = "testing";
    final Map<String, String> headers = new HashMap<>();

    final String body = "testing";
    final APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
            .withBody(body);

    final UploadFile uploadFileHandler = new UploadFile();
    final Context mockedContext = Mockito.mock(Context.class);
    final LambdaLogger lambdaLogger = LambdaRuntime.getLogger();

    @BeforeEach
    void initialize() {
        headers.put("file-name", fileName);
        headers.put("user-id", userId);
        requestEvent.setHeaders(headers);
    }

    @Test
    void handleRequestWorks() throws ValidationException {
        Mockito.when(mockedContext.getLogger()).thenReturn(lambdaLogger);
        uploadFileHandler.handleRequest(requestEvent, mockedContext);
    }
}
