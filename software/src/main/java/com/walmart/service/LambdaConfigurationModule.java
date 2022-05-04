package com.walmart.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class LambdaConfigurationModule {
    @Value("${ddb.tableName}")
    private String tableName;

    @Value("${s3.bucketName}")
    private String bucketName;

    @Value("${service.environment}")
    private String serviceEnvironment;

    private final String localstackEndpoint;
    public LambdaConfigurationModule(@Value("${service.localstack.endpointKey}") final String localstackKey) {
        final String localstackEnv = System.getenv(localstackKey);
        this.localstackEndpoint = localstackEnv != null ? localstackEnv : "http://localstack:4566";
    }
}
