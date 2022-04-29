package com.walmart.filequeriesservice.models;

import lombok.Data;

@Data
public class QueryRequest {
    private String httpMethod;
    private String body;
}

