package com.walmart.filequeriesservice.infrastructure;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class GatewayConfigurationModule {
    @Value("${service.environment}")
    private String serviceEnvironment;

    @Value("${ddb.tableName}")
    private String tableName;

    @Value("${s3.bucketName}")
    private String s3BucketName;

    @Value("${lambda.lambdaCodePath}")
    private String lambdaCodePath;

    @Value("${gateway.listFilesPath}")
    private String listFilesPath;

    @Value("${gateway.getFilePath}")
    private String getFilePath;

    @Value("${gateway.uploadFilePath}")
    private String uploadFilePath;
}
