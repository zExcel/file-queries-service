package com.walmart.service.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.walmart.service.errors.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
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
    void getFileTypeWorks() throws ValidationException {
        assert (RequestUtils.getFileType(fileName).equals("png"));
    }
}
