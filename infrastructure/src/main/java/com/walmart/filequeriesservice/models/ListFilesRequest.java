package com.walmart.filequeriesservice.models;

import com.walmart.filequeriesservice.FileQueriesServiceConfiguration;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@Getter
@NoArgsConstructor
@ToString
public class ListFilesRequest {
    private final UUID requestId = UUID.randomUUID();
    private final String nameContains = null;
    private final String name = null;

    public static ListFilesRequest requestFromJson(String json) {
        return FileQueriesServiceConfiguration.getGson().fromJson(json, ListFilesRequest.class);
    }
}
