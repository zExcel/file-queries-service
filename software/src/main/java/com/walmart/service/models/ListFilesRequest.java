package com.walmart.service.models;

import lombok.Data;

import java.time.Instant;

@Data
public class ListFilesRequest {
    public static String DEFAULT_CREATED_BEFORE = "2296-02-16T06:06:53.361255Z";
    public static String DEFAULT_CREATED_AFTER = Instant.EPOCH.toString();

    private String createdBefore = DEFAULT_CREATED_BEFORE;
    private String createdAfter = DEFAULT_CREATED_AFTER;
    private String nameBeginsWith = null;
    private int limit = 25;

    public boolean datesAreDefault() {
        return createdAfter.equals(DEFAULT_CREATED_AFTER) &&
                createdBefore.equals(DEFAULT_CREATED_BEFORE);
    }
}
