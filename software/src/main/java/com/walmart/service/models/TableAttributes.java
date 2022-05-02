package com.walmart.service.models;

public final class Table {
    // This is a unique id for a file.
    public static final String FILE_UUID = "fileUUID";

    // This is used for a global secondary index for easier querying capabilities. It is also the partition key
    public static final String USER_ID = "userID";

    // This is used as the sort key. Together with the USER_ID, they should uniquely identify a file.
    public static final String FILE_NAME = "fileName";

    // The date in which the item was created, uses the ISO-8601 format.
    public static final String CREATION_DATE = "createdAt";
}
