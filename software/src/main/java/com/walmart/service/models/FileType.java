package com.walmart.service.models;

import java.util.*;
import java.util.stream.Collectors;

public enum FileType {
    PDF("pdf"),
    JPEG("jpeg"),
    JPG("jpg"),
    PNG("png"),
    ;

    private static final Map<String, FileType> valueToType;
    private static final Set<String> fileTypes;
    private static final Set<String> images;
    private static final Set<String> applications;

    static {
        valueToType = Arrays.stream(FileType.values()).collect(Collectors.toMap(FileType::toString, file -> file));
        fileTypes = Arrays.stream(FileType.values()).map(FileType::toString).collect(Collectors.toSet());
        images = new HashSet<>(Arrays.asList(JPG.toString(), JPEG.toString(), PNG.toString()));
        applications = new HashSet<>(Arrays.asList(PDF.toString()));
    }

    private final String fileType;

    FileType(final String fileType) {
        this.fileType = fileType;
    }

    @Override
    public String toString() {
        return fileType;
    }

    public static boolean fileTypeExists(final String fileType) {
        return fileTypes.contains(fileType);
    }

    public static FileType fromString(final String fileType) {
        return valueToType.get(fileType);
    }

    public static String getMediaType(final FileType fileType) {
        return getMediaType(fileType.toString());
    }

    public static String getMediaType(final String fileType) {
        if (images.contains(fileType)) {
            return "image";
        } else if(applications.contains(fileType)) {
            return "application";
        }
        return "unknown";
    }
}
