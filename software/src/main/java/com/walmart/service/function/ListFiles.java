package com.walmart.service.function;

import com.walmart.service.LambdaConfigurationModule;
import com.walmart.service.models.*;
import com.walmart.service.util.DynamoDBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@EnableWebMvc
public class ListFiles {

    private static final Logger logger = LoggerFactory.getLogger(ListFiles.class);
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    @Autowired
    public ListFiles(final LambdaConfigurationModule configurationModule,
                     final DynamoDbClient dynamoDbClient) {
        this.tableName = configurationModule.getTableName();
        this.dynamoDbClient = dynamoDbClient;
    }

    private String getLastEvaluatedKey(final QueryResponse queryResponse) {
        if (!queryResponse.hasLastEvaluatedKey()) {
            return null;
        }
        logger.info("The actual last evaluated key = {}", queryResponse.lastEvaluatedKey());
        return queryResponse.lastEvaluatedKey().get(TableAttributes.FILE_ID_KEY).s();
    }

    private Map<String, AttributeValue> getLastEvalutedKeyItem(final String nextToken) {
        if (nextToken == null) {
            return null;
        }
        final File itemInfo = DynamoDBUtil.getFileFromDDB(nextToken, tableName, dynamoDbClient);
        return DynamoDBUtil.createAttributeValueMap(itemInfo.getFileUUID(), itemInfo.getOwnerID(),
                                              itemInfo.getFileName(), itemInfo.getCreationDate());
    }

    public List<String> getFileIdsFromQuery(final QueryResponse queryResponse) {
        final List<String> fileIds;
        if (!queryResponse.hasItems() || queryResponse.items().isEmpty()) {
            fileIds = new ArrayList<>();
        } else {
            fileIds = queryResponse.items().stream()
                    .map(item -> item.get(TableAttributes.FILE_ID_KEY).s())
                    .collect(Collectors.toList());
        }
        return fileIds;
    }

    /**
     * Helper function that will handle the paginated query responses until we reach the specified limit.
     *
     * @param queryRequest The initial query request being made to the table.
     * @param limit        The max number of file IDs to return.
     * @return A list of file IDs and potentially a lastEvaluatedKey value.
     */
    public ListFilesResponse repeatedQuerying(QueryRequest queryRequest,
                                              int limit) {
        logger.info("Last evaluated key that's being used = {}", queryRequest.exclusiveStartKey());
        QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
        final List<String> fileIds = getFileIdsFromQuery(queryResponse);
        limit = limit - queryResponse.count();

        while (queryResponse.hasLastEvaluatedKey() && limit > 0) {
            queryRequest = queryRequest.toBuilder()
                    .exclusiveStartKey(queryResponse.lastEvaluatedKey())
                    .limit(limit)
                    .build();
            queryResponse = dynamoDbClient.query(queryRequest);
            fileIds.addAll(getFileIdsFromQuery(queryResponse));
        }

        return new ListFilesResponse(fileIds, getLastEvaluatedKey(queryResponse));
    }

    /**
     * Returns all files owned by this user that start with a specific string and also fall within a time range.
     * The time range defaults to all-time if not specified.
     *
     * @param listFilesRequest A class that contains the prefix and also the date range.
     * @param userId           The ID of the user that owns the files.
     * @param lastEvaluatedKey Nullable. The partition key to start from for these queries.
     * @return A list of file IDs and potentially a lastEvaluatedKey value.
     */
    public ListFilesResponse queryByFileName(final ListFilesRequest listFilesRequest,
                                             final String userId,
                                             final Map<String, AttributeValue> lastEvaluatedKey) {
        final Map<String, AttributeValue> lastEvaluatedKeyFiltered = DynamoDBUtil.removeUnwantedKeys(lastEvaluatedKey,
                                                                                                     TableAttributes.FILE_NAME_KEY,
                                                                                                     TableAttributes.USER_ID_KEY,
                                                                                                     TableAttributes.FILE_ID_KEY);
        final String keyExpression = String.format("begins_with (%s, %s) and %s = %s",
                                                   TableAttributes.FILE_NAME_KEY, ":name",
                                                   TableAttributes.USER_ID_KEY, ":user");
        final String filterExpression = String.format("%s BETWEEN %s AND %s",
                                                      TableAttributes.CREATION_DATE_KEY, ":dateAfter", ":dateBefore");
        final Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":name", AttributeValue.builder().s(listFilesRequest.getNameBeginsWith()).build());
        expressionAttributeValues.put(":user", AttributeValue.builder().s(userId).build());
        expressionAttributeValues.put(":dateAfter", AttributeValue.builder().s(listFilesRequest.getCreatedAfter()).build());
        expressionAttributeValues.put(":dateBefore", AttributeValue.builder().s(listFilesRequest.getCreatedBefore()).build());

        final QueryRequest queryRequest = QueryRequest.builder()
                .keyConditionExpression(keyExpression)
                .filterExpression(filterExpression)
                .expressionAttributeValues(expressionAttributeValues)
                .limit(listFilesRequest.getLimit())
                .exclusiveStartKey(lastEvaluatedKeyFiltered)
                .indexName(TableAttributes.FILE_NAME_INDEX_KEY)
                .tableName(tableName)
                .build();
        return repeatedQuerying(queryRequest, listFilesRequest.getLimit());
    }

    /**
     * Returns all files owned by this user that fall within a specific time range.
     *
     * @param listFilesRequest A class that contains the date range.
     * @param userId           The ID of the user that owns the files.
     * @param lastEvaluatedKey Nullable. The partition key to start from for these queries.
     * @return A list of file IDs and potentially a lastEvaluatedKey value.
     */
    public ListFilesResponse queryByTimeRange(final ListFilesRequest listFilesRequest,
                                              final String userId,
                                              final Map<String, AttributeValue> lastEvaluatedKey) {

        final Map<String, AttributeValue> lastEvaluatedKeyFiltered = DynamoDBUtil.removeUnwantedKeys(lastEvaluatedKey,
                                                                                                     TableAttributes.CREATION_DATE_KEY,
                                                                                                     TableAttributes.USER_ID_KEY,
                                                                                                     TableAttributes.FILE_ID_KEY);
        final String keyExpression = String.format("%s BETWEEN %s AND %s and %s = %s",
                                                   TableAttributes.CREATION_DATE_KEY, ":dateAfter", ":dateBefore",
                                                   TableAttributes.USER_ID_KEY, ":user");
        final Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":dateAfter", AttributeValue.builder().s(listFilesRequest.getCreatedAfter()).build());
        expressionAttributeValues.put(":dateBefore", AttributeValue.builder().s(listFilesRequest.getCreatedBefore()).build());
        expressionAttributeValues.put(":user", AttributeValue.builder().s(userId).build());

        final QueryRequest queryRequest = QueryRequest.builder()
                .keyConditionExpression(keyExpression)
                .expressionAttributeValues(expressionAttributeValues)
                .limit(listFilesRequest.getLimit())
                .exclusiveStartKey(lastEvaluatedKeyFiltered)
                .indexName(TableAttributes.TIME_RANGE_INDEX_KEY)
                .tableName(tableName)
                .build();
        return repeatedQuerying(queryRequest, listFilesRequest.getLimit());
    }

    /**
     * Returns all files that are owned by a specified user.
     *
     * @param userId           The ID of the user that owns the files.
     * @param limit            A limit of how many file IDs to return.
     * @param lastEvaluatedKey Nullable. The partition key to start from for these queries.
     * @return A list of file IDs and potentially a lastEvaluatedKey value.
     */
    public ListFilesResponse queryByUser(final String userId,
                                         int limit,
                                         final Map<String, AttributeValue> lastEvaluatedKey) {

        final Map<String, AttributeValue> lastEvaluatedKeyFiltered = DynamoDBUtil.removeUnwantedKeys(lastEvaluatedKey,
                                                                                                     TableAttributes.USER_ID_KEY,
                                                                                                     TableAttributes.FILE_ID_KEY);
        final String keyExpression = String.format("%s = %s", TableAttributes.USER_ID_KEY, ":user");
        final Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":user", AttributeValue.builder().s(userId).build());

        final QueryRequest queryRequest = QueryRequest.builder()
                .keyConditionExpression(keyExpression)
                .expressionAttributeValues(expressionAttributeValues)
                .limit(limit)
                .exclusiveStartKey(lastEvaluatedKeyFiltered)
                .indexName(TableAttributes.USER_ID_INDEX_KEY)
                .tableName(tableName)
                .build();

        return repeatedQuerying(queryRequest, limit);
    }

    @PostMapping(path = "/listFiles/{userId}")
    public ListFilesResponse handleRequest(@RequestBody final ListFilesRequest listFilesRequest,
                                           @PathVariable("userId") final String userId,
                                           @RequestHeader(required = false, name = Header.NEXT_TOKEN) final String nextToken) {

        final Map<String, AttributeValue> lastEvaluatedKey = getLastEvalutedKeyItem(nextToken);
        logger.debug("Last Evaluated Key = {}", lastEvaluatedKey);
        try {
            final ListFilesResponse result;
            if (listFilesRequest.getNameBeginsWith() != null) {
                result = queryByFileName(listFilesRequest, userId, lastEvaluatedKey);
            } else if (!listFilesRequest.datesAreDefault()) {
                result = queryByTimeRange(listFilesRequest, userId, lastEvaluatedKey);
            } else {
                result = queryByUser(userId, listFilesRequest.getLimit(), lastEvaluatedKey);
            }

            logger.debug("List Files Result = {}", result);
            return result;
        } catch (final Exception e) {
            logger.error("Received an exception while processing {}", listFilesRequest, e);
            throw e;
        }
    }
}
