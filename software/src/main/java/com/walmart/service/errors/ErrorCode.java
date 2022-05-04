package com.walmart.service.errors;

import lombok.Getter;

@Getter
public enum ErrorCode {
    INTERNAL_SERVER_ERROR("Unexpected error when processing request", 500),
    MISSING_REQUIRED_HEADER("Missing Required Header", 400),
    MALFORMED_FILE_NAME("File Name Is Malformed", 400)
    ;

    private final String errorCode;
    private final int statusCode;

    ErrorCode(final String errorCode, final int statusCode) {
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }

    @Override
    public String toString() {
        return errorCode;
    }
}
