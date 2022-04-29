package com.walmart.filequeriesservice.models;

import org.junit.jupiter.api.Test;

public class RequestTests {
    private final String listFilesJson = "{\"name\": \"testing\"}";


    @Test
    public void listFilesDeserializesProperly() {
        ListFilesRequest listFilesRequest = ListFilesRequest.requestFromJson(listFilesJson);

        assert(listFilesRequest.getName() != null);
        assert(listFilesRequest.getName().equals("testing"));
        assert(listFilesRequest.getNameContains() == null);
    }
}
