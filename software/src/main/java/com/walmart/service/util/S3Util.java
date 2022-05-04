package com.walmart.service.util;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class S3Util {
    // The format is assumed to be userId/fileName
    private static final String S3_FILE_KEY_FORMAT = "%s/%s";

    public static ResponseInputStream<GetObjectResponse> getS3File(final String fileName,
                                         final String userId,
                                         final String bucketName,
                                         final S3Client s3Client) {
        final String key = String.format(S3_FILE_KEY_FORMAT, userId, fileName);
        final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .key(key)
                .bucket(bucketName)
                .build();
        return s3Client.getObject(getObjectRequest);
    }
}
