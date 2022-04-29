package com.walmart.filequeriesservice.infrastructure;

import com.walmart.filequeriesservice.util.EnvironmentKeys;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.assets.AssetOptions;
import software.constructs.Construct;
import software.amazon.awscdk.services.redshift.CfnCluster;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

public class GatewayStack extends Stack {
    private final GatewayConfigurationModule configurationModule;
    private static final String LAMBDA_ID_FORMAT = "%sLambda";
    private static final String CHANGE_DIR_FORMAT = "cd %sFunction";
    private static final String LAMBDA_JAR_LOCATION_FORMAT = "&& cp build/libs/%sFunction-1.0-all.jar /asset-output/";
    private static final String LAMBDA_HANDLER_FORMAT = "function.%s";

    public GatewayStack(final Construct scope, final String id, final GatewayConfigurationModule configurationModule) {
        this(scope, id, null, configurationModule);
    }


    /**
     * Creates a Lambda function using the format:
     * FunctionName: ${functionName}
     * id: ${functionName}Lambda
     * handler: ${functionName}.handleRequest
     * @param scope         Scope in which the function is being created.
     * @param functionName  Name of the function being created.
     * @return              A Lambda function.
     */

    public Function createFunction(final Construct scope, final String functionName, final Bucket bucket,
                                   final CfnCluster redshiftCluster) {
        List<String> functionPackagingInstructions = Arrays.asList(
                "/bin/sh",
                "-c",
                String.format(CHANGE_DIR_FORMAT, functionName) +
                        String.format(LAMBDA_JAR_LOCATION_FORMAT, functionName)
        );

        BundlingOptions builderOptions = BundlingOptions.builder()
                .command(functionPackagingInstructions)
                .image(Runtime.JAVA_8.getBundlingImage())
                .volumes(singletonList(
                        // Mount local .m2 repo to avoid download all the dependencies again inside the container
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()
                ))
                .user("root")
                .outputType(ARCHIVED)
                .build();

        return Function.Builder.create(scope, String.format(LAMBDA_ID_FORMAT, functionName))
                .functionName(functionName)
                .runtime(Runtime.JAVA_8)
                .code(Code.fromAsset("../software", AssetOptions.builder()
                        .bundling(builderOptions)
                        .build()))
                .handler(String.format(LAMBDA_HANDLER_FORMAT, functionName))
                .memorySize(1024)
                .timeout(Duration.seconds(60))
                .build()
                .addEnvironment(EnvironmentKeys.BUCKET_NAME.toString(), bucket.getBucketName())
                .addEnvironment(EnvironmentKeys.REDSHIFT_USERNAME.toString(), redshiftCluster.getMasterUsername())
                .addEnvironment(EnvironmentKeys.REDSHIFT_PASSWORD.toString(), redshiftCluster.getMasterUserPassword());
    }

    public GatewayStack(final Construct scope, final String id, final StackProps props, final GatewayConfigurationModule configurationModule) {
        super(scope, id, props);
        this.configurationModule = configurationModule;

        final Bucket bucket = Bucket.Builder.create(this, "QueriesServiceBucket")
                .bucketName(configurationModule.getS3BucketName()).build();

        final CfnCluster redshiftCluster = CfnCluster.Builder.create(this, "Redshift")
                .masterUsername(configurationModule.getRedshiftUsername())
                .masterUserPassword(configurationModule.getRedshiftPassword())
                .clusterType("single-node")
                .nodeType("ra3.xlplus")
                .dbName(configurationModule.getRedshiftDbName()).build();

        final Function listFilesFunction = createFunction(this, "ListFiles", bucket, redshiftCluster);
        final Function getFileFunction = createFunction(this, "GetFile", bucket, redshiftCluster);
        final Function uploadFileFunction = createFunction(this, "UploadFile", bucket, redshiftCluster);
        bucket.grantRead(listFilesFunction);
        bucket.grantRead(getFileFunction);
        bucket.grantWrite(uploadFileFunction);

        final RestApi api = RestApi.Builder.create(this, "FileQueries-API")
                .restApiName("File Queries Service").description("Service that lets users upload, download, and list files.")
                .build();

        final Resource listFilesResource = api.getRoot().addResource(configurationModule.getListFilesPath());
        final Resource getFileResource = api.getRoot().addResource(configurationModule.getGetFilePath());
        final Resource uploadFileResource = api.getRoot().addResource(configurationModule.getUploadFilePath());
        listFilesResource.addMethod("POST", new LambdaIntegration(listFilesFunction));
        getFileResource.addMethod("GET", new LambdaIntegration(getFileFunction));
        uploadFileResource.addMethod("POST", new LambdaIntegration(uploadFileFunction));

    }
}
