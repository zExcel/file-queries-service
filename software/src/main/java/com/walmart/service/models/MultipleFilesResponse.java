package com.walmart.service.models;

import lombok.*;

import java.util.List;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MultipleFilesResponse {

    List<File> successfulFiles;
    List<Pair> failedFiles;
}
