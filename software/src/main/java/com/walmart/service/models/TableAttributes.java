package com.walmart.service.models;

public final class TableAttributes {
    public static final String FILE_NAME_INDEX_KEY = "FileNameIndex";
    public static final String TIME_RANGE_INDEX_KEY = "TimeRangeIndex";
    public static final String USER_ID_INDEX_KEY = "UserIdIndex";

    // This is a unique id for a file. Used as the partition key in the table.
    public static final String FILE_ID_KEY = "FileUUID";

    // ID of the person who uploaded the file.
    public static final String USER_ID_KEY = "UserID";

    // Full name of the file that was upload (e.g. image.png).
    public static final String FILE_NAME_KEY = "FileName";

    // The date in which the item was created, uses the ISO-8601 format.
    public static final String CREATION_DATE_KEY = "CreatedAt";
}
