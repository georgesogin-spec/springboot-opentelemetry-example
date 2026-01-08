package com.example.demo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HelloController {

    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @GetMapping("/hello")
    public Map<String, Object> hello(@RequestParam(value = "name", defaultValue = "World") String name) {
        logger.info("Received request to /hello endpoint with name: {}", name);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello, " + name + "!");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "spring-boot-otel-demo");
        
        logger.debug("Preparing response for name: {}", name);
        logger.info("Successfully processed /hello request");
        
        return response;
    }

    @GetMapping("/")
    public Map<String, String> root() {
        logger.info("Received request to root endpoint");
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "running");
        response.put("message", "Spring Boot OpenTelemetry Demo Application");
        response.put("endpoints", "/hello, /health");
        
        logger.info("Root endpoint accessed successfully");
        
        return response;
    }

    @GetMapping("/test-logs")
    public Map<String, String> testLogs() {
        logger.info("Testing different log levels...");
        
        logger.trace("This is a TRACE level log");
        logger.debug("This is a DEBUG level log");
        logger.info("This is an INFO level log");
        logger.warn("This is a WARN level log");
        logger.error("This is an ERROR level log");
        
        logger.info("Log level test completed");
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Generated logs at all levels (TRACE, DEBUG, INFO, WARN, ERROR)");
        response.put("note", "Check your OTLP endpoint for the exported logs");
        
        return response;
    }
}
