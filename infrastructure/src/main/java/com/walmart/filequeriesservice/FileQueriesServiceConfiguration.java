package com.walmart.filequeriesservice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileQueriesServiceConfiguration {
    private static final Gson gson = new GsonBuilder().create();

    public static Gson getGson() {
        return gson;
    }
}
