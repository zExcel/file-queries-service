package com.walmart.service.models;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListFilesRequest {
    public static String DEFAULT_CREATED_BEFORE = "2296-02-16T06:06:53.361255Z";
    public static String DEFAULT_CREATED_AFTER = Instant.EPOCH.toString();

    @Builder.Default
    private String createdBefore = DEFAULT_CREATED_BEFORE;
    @Builder.Default
    private String createdAfter = DEFAULT_CREATED_AFTER;
    @Builder.Default
    private String nameBeginsWith = null;
    @Builder.Default
    private int limit = 25;

    public boolean datesAreDefault() {
        return createdAfter.equals(DEFAULT_CREATED_AFTER) &&
                createdBefore.equals(DEFAULT_CREATED_BEFORE);
    }
}
