package com.walmart.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;

@SpringBootApplication
// Using @Import instead of @ComponentScan speeds up cold starts
@Import({ LambdaConfigurationModule.class })
public class LambdaApplication {

    /*
     * Create required HandlerMapping, to avoid several default HandlerMapping instances being created
     */
    @Bean
    public HandlerMapping handlerMapping() {
        return new RequestMappingHandlerMapping();
    }

    /*
     * Create required HandlerAdapter, to avoid several default HandlerAdapter instances being created
     */
    @Bean
    public HandlerAdapter handlerAdapter() {
        return new RequestMappingHandlerAdapter();
    }

    /*
     * optimization - avoids creating default exception resolvers; not required as the serverless container handles
     * all exceptions
     *
     * By default, an ExceptionHandlerExceptionResolver is created which creates many dependent object, including
     * an expensive ObjectMapper instance.
     *
     * To enable custom @ControllerAdvice classes remove this bean.
     */
    @Bean
    public HandlerExceptionResolver handlerExceptionResolver() {
        return new HandlerExceptionResolver() {

            @Override
            public ModelAndView resolveException(final HttpServletRequest request, final HttpServletResponse response, final Object handler, final Exception ex) {
                return null;
            }
        };
    }

    @Bean
    @Autowired
    public S3Client getS3Client(final LambdaConfigurationModule configurationModule) {
        if (configurationModule.getServiceEnvironment().equals("local")) {
            return S3Client.builder()
                    .endpointOverride(URI.create(configurationModule.getLocalstackEndpoint()))
                    .region(Region.US_EAST_1)
                    .build();
        } else {
            return S3Client.create();
        }
    }

    @Bean
    @Autowired
    public DynamoDbClient getDynamoDbClient(final LambdaConfigurationModule configurationModule) {
        if (configurationModule.getServiceEnvironment().equals("local")) {
            return DynamoDbClient.builder()
                    .endpointOverride(URI.create(configurationModule.getLocalstackEndpoint()))
                    .region(Region.US_EAST_1)
                    .build();
        } else {
            return DynamoDbClient.create();
        }
    }

    @Bean
    public CommonsMultipartResolver multipartResolver() {
        final CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
        multipartResolver.setMaxUploadSize(-1);
        return multipartResolver;
    }

    public static void main(String[] args) {
        SpringApplication.run(LambdaApplication.class, args);
    }
}
