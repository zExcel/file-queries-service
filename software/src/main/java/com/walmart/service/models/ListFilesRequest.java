package com.walmart.service.models;

import com.google.gson.Gson;
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

    public static ListFilesRequest requestFromJson(final String json, final Gson gson) {
        return gson.fromJson(json, ListFilesRequest.class);
    }
}
