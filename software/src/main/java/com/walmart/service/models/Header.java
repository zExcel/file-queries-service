package com.walmart.service.models;

import com.walmart.service.errors.ErrorCode;

import java.util.*;
import java.util.stream.Collectors;

public final class Header {

    // A UUID of the user making the request
    public static final String USER_ID = "user-id";
    // Name of the file to store the data under, must be of the format name.type where type == pdf, jpg, png, or jpeg
    public static final String FILE_NAME = "file-name";
    // The content type of the request. In the case of UploadFile, this should be multipart/form-data.
    public static final String CONTENT_TYPE = "content-type";
    // The token the request should continue from.
    public static final String NEXT_TOKEN = "next-token";

    /**
     * Verifies that the specified headers are contained within the headers map passed in. Headers should be case-insensitive,
     * so HEADER_ID == header_id.
     * @param headersMap    The map containing the headers.
     * @param headers       The headers that the map must contain.
     * @return              A list of all the missing headers.
     */
    public static ArrayList<String> validateHeadersExist(final Map<String, String> headersMap, final String ...headers) {
        final Set<String> lowercaseHeaders = headersMap.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());
        ArrayList<String> missingHeaders = new ArrayList<>();

        for (final String header: headers) {
            if (!lowercaseHeaders.contains(header)) {
                missingHeaders.add(header);
            }
        }
        return missingHeaders;
    }
}