package com.walmart.filequeriesservice.infrastructure;

import com.walmart.filequeriesservice.util.EnvironmentKeys;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.*;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.ApiGateway;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.Objects;

import static java.util.Collections.singletonList;

public class GatewayStack extends Stack {

    private final GatewayConfigurationModule configurationModule;
    private static final String LAMBDA_ID_FORMAT = "%sLambda";
    private static final String RELATIVE_JAR_LOCATION = "../software/build/libs/software-1.0-all.jar";
    private static final String LAMBDA_HANDLER_FORMAT = "com.walmart.service.StreamLambdaHandler";

    /**
     * Creates a Lambda com.walmart.service.function using the format:
     * FunctionName: ${functionName}
     * id: ${functionName}Lambda
     * handler: ${functionName}.handleRequest
     * @param functionName  Name of the function being created.
     * @return              A Lambda function.
     */
    public Function createFunction(final String functionName, final Bucket bucket,
                                   final Table table) {

        return Function.Builder.create(this, String.format(LAMBDA_ID_FORMAT, functionName))
                .functionName(functionName)
                .runtime(Runtime.JAVA_8)
                .code(Code.fromAsset(RELATIVE_JAR_LOCATION))
                .handler(LAMBDA_HANDLER_FORMAT)
                .memorySize(1024)
                .timeout(Duration.seconds(60))
                .build()
                .addEnvironment(EnvironmentKeys.BUCKET_NAME, bucket.getBucketName())
                .addEnvironment(EnvironmentKeys.SERVICE_ENVIRONMENT, configurationModule.getServiceEnvironment())
                .addEnvironment(EnvironmentKeys.TABLE_NAME, Objects.requireNonNull(table.getTableName()));
    }

    public Table createTable() {
        final Table queriesTable = Table.Builder.create(this, "QueriesServiceTable")
                .tableName(configurationModule.getTableName())
                .partitionKey(Attribute.builder()
                                      .name(TableAttributes.FILE_ID_KEY)
                                      .type(AttributeType.STRING)
                                      .build())
                .build();
        queriesTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                                                     .indexName(TableAttributes.USER_ID_INDEX_KEY)
                                                     .partitionKey(Attribute.builder()
                                                                           .name(TableAttributes.USER_ID_KEY)
                                                                           .type(AttributeType.STRING)
                                                                           .build())
                                                     .projectionType(ProjectionType.KEYS_ONLY)
                                                     .build());
        queriesTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                                                     .indexName(TableAttributes.TIME_RANGE_INDEX_KEY)
                                                     .partitionKey(Attribute.builder()
                                                                           .name(TableAttributes.USER_ID_KEY)
                                                                           .type(AttributeType.STRING)
                                                                           .build())
                                                     .sortKey(Attribute.builder()
                                                                      .name(TableAttributes.CREATION_DATE_KEY)
                                                                      .type(AttributeType.STRING)
                                                                      .build())
                                                     .projectionType(ProjectionType.KEYS_ONLY)
                                                     .build());
        queriesTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                                                     .indexName(TableAttributes.FILE_NAME_INDEX_KEY)
                                                     .partitionKey(Attribute.builder()
                                                                           .name(TableAttributes.USER_ID_KEY)
                                                                           .type(AttributeType.STRING)
                                                                           .build())
                                                     .sortKey(Attribute.builder()
                                                                      .name(TableAttributes.FILE_NAME_KEY)
                                                                      .type(AttributeType.STRING)
                                                                      .build())
                                                     .nonKeyAttributes(Arrays.asList(TableAttributes.CREATION_DATE_KEY))
                                                     .projectionType(ProjectionType.INCLUDE)
                                                     .build());
        return queriesTable;
    }

    public GatewayStack(final Construct scope, final String id, final StackProps props, final GatewayConfigurationModule configurationModule) {
        super(scope, id, props);
        this.configurationModule = configurationModule;

        final Bucket bucket = Bucket.Builder.create(this, "QueriesServiceBucket")
                .bucketName(configurationModule.getS3BucketName())
                .build();
        final Table queriesTable = createTable();

        final Function functionHandler = createFunction("FunctionHandler", bucket, queriesTable);
        final Rule functionWarmerRule = Rule.Builder.create(this, "FunctionWarmer")
                .schedule(Schedule.rate(Duration.minutes(1)))
                .build();
        queriesTable.grantReadWriteData(functionHandler.getRole());

        final RestApi api = LambdaRestApi.Builder.create(this, "FileQueries-API")
                .restApiName("File Queries Service")
                .description("Service that lets users upload, download, and list files.")
                .proxy(true)
                .handler(functionHandler)
                .binaryMediaTypes(singletonList("multipart/form-data"))
                .build();

        functionWarmerRule.addTarget(ApiGateway.Builder.create(api)
                                             .method("GET")
                                             .stage(api.getDeploymentStage().getStageName())
                                             .build());

    }
}
