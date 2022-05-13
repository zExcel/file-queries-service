package com.walmart.service.util;

import com.walmart.service.models.File;
import com.walmart.service.models.TableAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

public class DynamoDBUtil {
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBUtil.class);

    /**
     * Helper function to retrieve all information about a file in the database.
     *
     * @param fileId The ID of the file being retrieved.
     * @return The file if found.
     */
    public static File getFileFromDDB(final String fileId,
                                      final String tableName,
                                      final DynamoDbClient dynamoDbClient) {
        final GetItemRequest getItemRequest = GetItemRequest.builder()
                .key(Collections.singletonMap(TableAttributes.FILE_ID_KEY, AttributeValue.builder().s(fileId).build()))
                .tableName(tableName)
                .build();
        final GetItemResponse getItemResponse = dynamoDbClient.getItem(getItemRequest);
        if (!getItemResponse.hasItem()) {
            logger.warn("No files were found with file ID = {}", fileId);
            return new File();
        }
        final Map<String, AttributeValue> item = getItemResponse.item();
        return new File(item.get(TableAttributes.FILE_NAME_KEY).s(),
                        item.get(TableAttributes.FILE_ID_KEY).s(),
                        item.get(TableAttributes.USER_ID_KEY).s(),
                        item.get(TableAttributes.CREATION_DATE_KEY).s());
    }

    public static File getFileFromDDB(final String userId,
                                      final String fileName,
                                      final String tableName,
                                      final DynamoDbClient dynamoDbClient) {
        final String keyExpression = String.format("%s = %s and %s = %s",
                                                   TableAttributes.FILE_NAME_KEY, ":name",
                                                   TableAttributes.USER_ID_KEY, ":user");
        final Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":name", AttributeValue.builder().s(fileName).build());
        expressionAttributeValues.put(":user", AttributeValue.builder().s(userId).build());

        final QueryRequest queryRequest = QueryRequest.builder()
                .keyConditionExpression(keyExpression)
                .expressionAttributeValues(expressionAttributeValues)
                .indexName(TableAttributes.FILE_NAME_INDEX_KEY)
                .tableName(tableName)
                .build();
        final QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
        if (!queryResponse.hasItems() || queryResponse.items().isEmpty()) {
            logger.warn("Was unable to find any items with file name = {} and user ID = {}", fileName, userId);
            return new File();
        }

        final Map<String, AttributeValue> item = queryResponse.items().get(0);
        return new File(item.get(TableAttributes.FILE_NAME_KEY).s(),
                        item.get(TableAttributes.FILE_ID_KEY).s(),
                        item.get(TableAttributes.USER_ID_KEY).s(),
                        item.get(TableAttributes.CREATION_DATE_KEY).s());
    }

    /**
     * DynamoDBs have an annoying feature when doing pagination for queries where the {@link QueryRequest#exclusiveStartKey()}
     * can only contain keys relevant to the index being used (e.g. the partition key, and any keys specified for the GSI).
     *
     * @param attributeValueMap The original attribute map, probably created from {@link #createAttributeValueMap}
     * @param relevantKeys      The keys that are relevant to the index.
     */
    public static Map<String, AttributeValue> removeUnwantedKeys(final Map<String, AttributeValue> attributeValueMap,
                                          final String... relevantKeys) {
        if (attributeValueMap == null) {
            return null;
        }
        Map<String, AttributeValue> wantedKeys = new HashMap<>();
        for (final String key: relevantKeys) {
            if (attributeValueMap.containsKey(key)) {
                wantedKeys.put(key, attributeValueMap.get(key));
            }
        }
        return wantedKeys;
    }

    /**
     * Utility function to create a representation of a file in DDB style. See {@link File}
     *
     * @return The DDB representation of a file's info.
     */
    public static Map<String, AttributeValue> createAttributeValueMap(@NonNull final String fileId,
                                                                      final String userId,
                                                                      final String fileName,
                                                                      final String creationDate) {
        final Map<String, AttributeValue> attributeValueMap = new HashMap<>();
        attributeValueMap.put(TableAttributes.FILE_ID_KEY, AttributeValue.builder().s(fileId).build());
        attributeValueMap.put(TableAttributes.USER_ID_KEY, AttributeValue.builder().s(userId).build());
        attributeValueMap.put(TableAttributes.FILE_NAME_KEY, AttributeValue.builder().s(fileName).build());
        attributeValueMap.put(TableAttributes.CREATION_DATE_KEY, AttributeValue.builder().s(creationDate).build());
        return attributeValueMap;
    }
}
