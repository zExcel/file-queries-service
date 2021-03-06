package com.walmart.service.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.walmart.service.LambdaApplication;
import com.walmart.service.TestTypes;
import com.walmart.service.errors.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@ContextConfiguration(classes = LambdaApplication.class)
public class RequestUtilsTests {
    final String fileName = "testing.png";
    final String userId = "testing";
    final Map<String, String> headers = new HashMap<>();

    final String body = "testing";
    final APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent()
            .withBody(body);

    @BeforeEach
    void initialize() {
        headers.put("file-name", fileName);
        headers.put("user-id", userId);
        requestEvent.setHeaders(headers);
    }

    @Test
    @Tag(TestTypes.UNIT_TEST)
    void getFileTypeWorks() throws ValidationException {
        assert (RequestUtils.getFileType(fileName).toString().equals("png"));
    }
}
