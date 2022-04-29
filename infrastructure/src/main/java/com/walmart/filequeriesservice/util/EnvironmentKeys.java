package com.walmart.filequeriesservice.util;

public enum EnvironmentKeys {
    BUCKET_NAME("BUCKET_NAME"),
    REDSHIFT_USERNAME("REDSHIFT_USERNAME"),
    REDSHIFT_PASSWORD("REDSHIFT_PASSWORD")
    ;

    private final String key;

    EnvironmentKeys(final String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }
}
