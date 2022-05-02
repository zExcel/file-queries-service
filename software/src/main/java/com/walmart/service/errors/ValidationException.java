package com.walmart.service.errors;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;

@Getter
public class ValidationException extends Exception {

    private final ArrayList<ErrorCode> errorCodes;

    public ValidationException(final ErrorCode... errorCodes) {
        super();
        this.errorCodes =  new ArrayList<>(Arrays.asList(errorCodes));
    }

    public ValidationException(final String message, final Throwable cause, final ErrorCode... errorCodes) {
        super(message, cause);
        this.errorCodes =  new ArrayList<>(Arrays.asList(errorCodes));
    }

    public ValidationException(final String message, final ErrorCode... errorCodes) {
        super(message);
        this.errorCodes =  new ArrayList<>(Arrays.asList(errorCodes));
    }

    public ValidationException(final Throwable cause, final ErrorCode... errorCodes) {
        super(cause);
        this.errorCodes = new ArrayList<>(Arrays.asList(errorCodes));
    }

    @Override
    public String toString() {
        final String exceptionString = super.toString();
        return String.format("Unable to validate the request: %s. %s", errorCodes.toString(), exceptionString);
    }
}
