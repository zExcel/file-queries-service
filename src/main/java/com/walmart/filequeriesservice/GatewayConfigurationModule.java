package com.walmart.filequeriesservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfigurationModule {

    @Value("${testing.test}")
    private String testValue;

    public String getTestValue() {
        return testValue;
    }
}
