package com.walmart.service.models;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteFilesRequest {
    @Builder.Default
    private List<String> fileIds = new ArrayList<>();
    @Builder.Default
    private List<String> fileNames = new ArrayList<>();
}
