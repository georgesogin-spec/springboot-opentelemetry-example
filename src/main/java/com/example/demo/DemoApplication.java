package com.example.demo;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class DemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

    @Value("${otel.exporter.otlp.endpoint:http://localhost:4318}")
    private String otlpEndpoint;

    @Value("${otel.service.name:spring-boot-otel-demo}")
    private String serviceName;

    public static void main(String[] args) {
        logger.info("Starting Spring Boot OpenTelemetry Demo Application...");
        SpringApplication.run(DemoApplication.class, args);
        logger.info("Application started successfully!");
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing OpenTelemetry with OTLP endpoint: {}", otlpEndpoint);
        logger.info("Service name: {}", serviceName);
    }

    @Bean
    public OpenTelemetry openTelemetry() {
        logger.info("Configuring OpenTelemetry SDK...");
        
        // Create resource with service information
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.builder()
                        .put(AttributeKey.stringKey("service.name"), serviceName)
                        .put(AttributeKey.stringKey("service.version"), "1.0.0")
                        .put(AttributeKey.stringKey("deployment.environment"), "development")
                        .build()));

        // Configure OTLP Log Exporter
        OtlpGrpcLogRecordExporter logExporter = OtlpGrpcLogRecordExporter.builder()
                .setEndpoint(otlpEndpoint)
                .build();

        // Create SdkLoggerProvider with batch processor
        SdkLoggerProvider sdkLoggerProvider = SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(BatchLogRecordProcessor.builder(logExporter).build())
                .build();

        // Build OpenTelemetry SDK
        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setLoggerProvider(sdkLoggerProvider)
                .build();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down OpenTelemetry SDK...");
            sdkLoggerProvider.close();
        }));

        logger.info("OpenTelemetry SDK configured successfully!");
        logger.debug("OTLP endpoint: {}", otlpEndpoint);
        logger.debug("Service name: {}", serviceName);

        return openTelemetrySdk;
    }
}
