package com.walmart.service.util;

import com.walmart.service.errors.ErrorCode;
import com.walmart.service.errors.ValidationException;
import com.walmart.service.models.FileType;
import com.walmart.service.models.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;

public class RequestUtils {

    final static Logger logger = LoggerFactory.getLogger(RequestUtils.class);

    public static FileType validateFileName(final String fileName) throws ValidationException {
        try {
            logger.debug("File name = {}", fileName);
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

    public static FileType getFileType(final String fileName) throws ValidationException {
        return validateFileName(fileName);
    }

    /**
     * Given a request, we validate the request and make sure it has the proper headers.
     *
     * @param headers The headers that the Lambda receives from the Gateway API.
     */
    public static void validateRequest(final Map<String, String> headers, final String... requiredHeaders) throws ValidationException {
        final ArrayList<String> missingHeaders = Header.validateHeadersExist(headers, requiredHeaders);
        if (!missingHeaders.isEmpty()) {
            throw new ValidationException(String.format("Upload File endpoint is missing required headers: %s", missingHeaders), ErrorCode.MISSING_REQUIRED_HEADER);
        }

        validateFileName(headers.get(Header.FILE_NAME));
    }
}
