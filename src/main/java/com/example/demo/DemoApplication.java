package com.example.demo;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

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

    private OpenTelemetrySdk openTelemetrySdk;

    @Bean
    public OpenTelemetry openTelemetry() {
        logger.info("Configuring OpenTelemetry SDK for log and trace forwarding...");
        
        // Create resource with service information using semantic conventions
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.builder()
                        .put(ResourceAttributes.SERVICE_NAME, serviceName)
                        .put(ResourceAttributes.SERVICE_VERSION, "1.0.0")
                        .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, "development")
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

        // Configure OTLP Span Exporter for traces
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .build();

        // Create SdkTracerProvider with batch processor
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .build();

        // Configure B3 propagator for distributed tracing compatibility
        // B3 is compatible with Temporal, Zipkin, Jaeger, and other distributed systems
        ContextPropagators contextPropagators = ContextPropagators.create(
                B3Propagator.injectingMultiHeaders()
        );

        // Build OpenTelemetry SDK with logger provider, tracer provider, and propagators
        // Register globally so the Logback appender and other components can access it
        openTelemetrySdk = OpenTelemetrySdk.builder()
                .setLoggerProvider(sdkLoggerProvider)
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(contextPropagators)
                .buildAndRegisterGlobal();

        logger.info("OpenTelemetry SDK configured successfully!");
        logger.info("Log forwarding enabled to OTLP endpoint");
        logger.info("Trace forwarding enabled to OTLP endpoint");
        logger.info("B3 context propagation enabled for distributed tracing");
        logger.debug("OTLP endpoint: {}", otlpEndpoint);
        logger.debug("Service name: {}", serviceName);

        return openTelemetrySdk;
    }

    @PreDestroy
    public void cleanup() {
        if (openTelemetrySdk != null) {
            logger.info("Shutting down OpenTelemetry SDK...");
            openTelemetrySdk.close();
            logger.info("OpenTelemetry SDK shutdown complete");
        }
    }
}
