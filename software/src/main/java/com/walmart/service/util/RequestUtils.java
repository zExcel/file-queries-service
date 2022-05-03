package com.walmart.service.util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.walmart.service.errors.ErrorCode;
import com.walmart.service.errors.ValidationException;
import com.walmart.service.models.FileType;
import com.walmart.service.models.Header;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RequestUtils {

    /**
     * Headers should be treated as being case-insensitive, but the Gateway API doesn't automatically handle this for us,
     * so this utility function handles that for us.
     * @param event The event received from the Gateway API.
     * @return      A headers map where all the keys are now lowercase.
     */
    public static Map<String, String> transformEventHeaders(final APIGatewayProxyRequestEvent event) {
        final Map<String, String> preTransformHeaders = event.getHeaders();
        final Map<String, String> transformedHeaders = new HashMap<>();
        for (final String header: preTransformHeaders.keySet()) {
            final String value = preTransformHeaders.get(header);
            transformedHeaders.put(header.toLowerCase(), value);
        }
        return transformedHeaders;
    }

    private static FileType validateFileName(final String fileName) throws ValidationException {
        try {
            System.out.println(fileName);
            final String[] fileParts = fileName.split("\\.");
            final String fileType = fileParts[1];
            assert fileParts.length == 2;
            assert FileType.fileTypeExists(fileType);
            return FileType.fromString(fileType);
        } catch (final Exception | AssertionError e) {
            e.printStackTrace();
            throw new ValidationException("file-name must be of the form NAME.TYPE", e, ErrorCode.MALFORMED_FILE_NAME);
        }
    }

    public static String getFileType(final String fileName) throws ValidationException {
        return validateFileName(fileName).toString();
    }

    /**
     * Given a request, we validate the request and make sure it has the proper headers.
     *
     * @param event The event that the Lambda receives from the Gateway API.
     */
    public static void validateRequest(final APIGatewayProxyRequestEvent event, final String... requiredHeaders) throws ValidationException {
        final Map<String, String> headers = event.getHeaders();
        final ArrayList<String> missingHeaders = Header.validateHeadersExist(headers, requiredHeaders);
        if (!missingHeaders.isEmpty()) {
            throw new ValidationException(String.format("Upload File endpoint is missing required headers: %s", missingHeaders), ErrorCode.MISSING_REQUIRED_HEADER);
        }

        validateFileName(headers.get(Header.FILE_NAME));
    }
}
