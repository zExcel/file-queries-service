package com.walmart.service.models;

public final class Header {

    // The content type of the request. In the case of UploadFile, this should be multipart/form-data.
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String CONTENT_LENGTH = "Content-Length";
    // The token the request should continue from.
    public static final String NEXT_TOKEN = "Next-Token";
}