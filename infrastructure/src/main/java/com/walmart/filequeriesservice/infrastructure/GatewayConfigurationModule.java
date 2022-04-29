package com.walmart.filequeriesservice.infrastructure;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class GatewayConfigurationModule {

    @Value("${redshift.dbName}")
    private String redshiftDbName;

    @Value("${redshift.username}")
    private String redshiftUsername;

    @Value("${redshift.password}")
    private String redshiftPassword;

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
