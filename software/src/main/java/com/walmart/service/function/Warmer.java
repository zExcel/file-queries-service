package com.walmart.service.function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@RestController
@EnableWebMvc
public class Warmer {

    private static final Logger logger = LoggerFactory.getLogger(Warmer.class);

    @GetMapping("/")
    public String warmFunction() {
        logger.info("Warming Lambda function.");
        return "Warmed up Lambda";
    }
}
