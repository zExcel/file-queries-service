package com.walmart.service.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@ToString
@Getter
public class ListFilesResponse {
    private List<String> fileIDs;
    private String nextToken;
}
