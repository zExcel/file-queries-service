package com.walmart.service.util;

import com.walmart.service.LambdaApplication;
import com.walmart.service.TestTypes;
import com.walmart.service.models.File;
import com.walmart.service.models.TableAttributes;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration(classes = LambdaApplication.class)
public class DynamoDBUtilTests {
    private final String fileId = "fake-file-id";
    private final String fileName = "fake-file-name";
    private final String userId = "fake-user-id";
    private final String creationDate = "fake-creation-date";
    private final String tableName = "fake-table-name";
    private final DynamoDbClient dbClient = mock(DynamoDbClient.class);


    @Test
    @Tag(TestTypes.UNIT_TEST)
    public void testGetFileFromDDBWithGoodFileID() {
        final Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put(TableAttributes.FILE_ID_KEY, AttributeValue.builder().s(fileId).build());
        itemMap.put(TableAttributes.FILE_NAME_KEY, AttributeValue.builder().s(fileName).build());
        itemMap.put(TableAttributes.USER_ID_KEY, AttributeValue.builder().s(userId).build());
        itemMap.put(TableAttributes.CREATION_DATE_KEY, AttributeValue.builder().s(creationDate).build());
        final GetItemResponse getItemResponse = GetItemResponse.builder()
                .item(itemMap)
                .build();
        when(dbClient.getItem(any(GetItemRequest.class))).thenReturn(getItemResponse);

        final File testFile = DynamoDBUtil.getFileFromDDB(fileId, tableName, dbClient);

        assertEquals(testFile.getFileUUID(), fileId);
        assertEquals(testFile.getFileName(), fileName);
        assertEquals(testFile.getOwnerID(), userId);
        assertEquals(testFile.getCreationDate(), creationDate);
    }

    @Test
    @Tag(TestTypes.UNIT_TEST)
    public void testGetFileFromDDBWithBadFileID() {
        final GetItemResponse getItemResponse = GetItemResponse.builder().build();
        when(dbClient.getItem(any(GetItemRequest.class))).thenReturn(getItemResponse);

        final File testFile = DynamoDBUtil.getFileFromDDB(fileId, tableName, dbClient);

        assertNull(testFile.getFileUUID());
        assertNull(testFile.getFileName());
        assertNull(testFile.getOwnerID());
        assertNull(testFile.getCreationDate());
    }

    @Test
    @Tag(TestTypes.UNIT_TEST)
    public void testGetFileFromDDBWithGoodUserID() {
        final Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put(TableAttributes.FILE_ID_KEY, AttributeValue.builder().s(fileId).build());
        itemMap.put(TableAttributes.FILE_NAME_KEY, AttributeValue.builder().s(fileName).build());
        itemMap.put(TableAttributes.USER_ID_KEY, AttributeValue.builder().s(userId).build());
        itemMap.put(TableAttributes.CREATION_DATE_KEY, AttributeValue.builder().s(creationDate).build());
        final QueryResponse queryResponse = QueryResponse.builder()
                .items(Collections.singletonList(itemMap))
                .build();
        when(dbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        final File testFile = DynamoDBUtil.getFileFromDDB(userId, fileName, tableName, dbClient);

        assertEquals(testFile.getFileUUID(), fileId);
        assertEquals(testFile.getFileName(), fileName);
        assertEquals(testFile.getOwnerID(), userId);
        assertEquals(testFile.getCreationDate(), creationDate);
    }

    @Test
    @Tag(TestTypes.UNIT_TEST)
    public void testGetFileFromDDBWithBadUserID() {
        final QueryResponse queryResponse = QueryResponse.builder().build();
        when(dbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);

        final File testFile = DynamoDBUtil.getFileFromDDB(userId, fileName, tableName, dbClient);

        assertNull(testFile.getFileUUID());
        assertNull(testFile.getFileName());
        assertNull(testFile.getOwnerID());
        assertNull(testFile.getCreationDate());
    }

    @Test
    @Tag(TestTypes.UNIT_TEST)
    public void removeUnwantedKeysWorks() {
        final Map<String, AttributeValue> allKeys = new HashMap<>();
        allKeys.put(fileId, AttributeValue.builder().s(fileId).build());
        allKeys.put(fileName, AttributeValue.builder().s(fileName).build());
        allKeys.put(userId, AttributeValue.builder().s(userId).build());
        allKeys.put(creationDate, AttributeValue.builder().s(creationDate).build());

        final Map<String, AttributeValue> filteredKeys = DynamoDBUtil.removeUnwantedKeys(allKeys, fileId, fileName);
        assertTrue(filteredKeys.containsKey(fileId));
        assertTrue(filteredKeys.containsKey(fileName));
        assertFalse(filteredKeys.containsKey(userId));
        assertFalse(filteredKeys.containsKey(creationDate));
        assertEquals(2, filteredKeys.size());
    }
}
