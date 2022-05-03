package com.walmart.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
@Getter
public class LambdaConfigurationModule {
    @Value("${ddb.tableName}")
    private String tableName;

    @Value("${s3.bucketName}")
    private String bucketName;

    @Value("${service.environment}")
    private String serviceEnvironment;

    @Bean
    public S3Client getS3Client() {
        if (this.serviceEnvironment.equals("local")) {
            return S3Client.builder().endpointOverride(URI.create("http://localstack:4566")).region(Region.US_EAST_1).build();
        } else {
            return S3Client.create();
        }
    }

    @Bean
    public DynamoDbClient getDynamoDbClient() {
        if (this.serviceEnvironment.equals("local")) {
            return DynamoDbClient.builder().endpointOverride(URI.create("http://localstack:4566")).region(Region.US_EAST_1).build();
        } else {
            return DynamoDbClient.create();
        }
    }
}
