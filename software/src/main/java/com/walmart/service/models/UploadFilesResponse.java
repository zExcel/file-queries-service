package com.walmart.service.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor
public class UploadFilesResponse {
    final List<File> successfulFiles;
    final List<String> failedFiles;
}
