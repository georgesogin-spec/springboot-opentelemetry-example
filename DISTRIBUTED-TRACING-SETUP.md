# OpenTelemetry Distributed Tracing Setup

## Overview

This document describes the OpenTelemetry instrumentation setup for distributed environments, including Temporal workers and other distributed systems.

## Key Changes for Distributed Tracing

### 1. OpenTelemetry Logback Appender

**Problem**: Logs were only going to console, not being bridged to OpenTelemetry for distributed tracing.

**Solution**: Added `OpenTelemetryAppender` to Logback configuration to automatically bridge SLF4J logs to OpenTelemetry with trace context.

**Configuration** (`logback-spring.xml`):
```xml
<appender name="OTEL" class="io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender">
    <captureExperimentalAttributes>true</captureExperimentalAttributes>
    <captureMdcAttributes>*</captureMdcAttributes>
    <captureKeyValuePairAttributes>true</captureKeyValuePairAttributes>
    <captureCodeAttributes>true</captureCodeAttributes>
    <captureMarkerAttribute>true</captureMarkerAttribute>
</appender>
```

### 2. Global OpenTelemetry Instance

**Problem**: OpenTelemetry SDK was not registered globally, preventing context propagation across threads and distributed systems.

**Solution**: Used `buildAndRegisterGlobal()` to register the OpenTelemetry SDK as the global instance.

**Code** (`DemoApplication.java`):
```java
openTelemetrySdk = OpenTelemetrySdk.builder()
    .setTracerProvider(sdkTracerProvider)
    .setLoggerProvider(sdkLoggerProvider)
    .setPropagators(contextPropagators)
    .buildAndRegisterGlobal();
```

### 3. Tracer Provider for Distributed Tracing

**Problem**: Only logs were being exported; no trace spans were being created or propagated.

**Solution**: Added `SdkTracerProvider` with OTLP span exporter to create and export trace spans.

**Code**:
```java
OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
    .setEndpoint(otlpEndpoint)
    .build();

SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
    .setResource(resource)
    .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
    .build();
```

### 4. B3 Context Propagation

**Problem**: Trace context was not being propagated across service boundaries, breaking distributed tracing.

**Solution**: Configured B3 propagator for compatibility with Temporal, Zipkin, and other distributed systems.

**Code**:
```java
ContextPropagators contextPropagators = ContextPropagators.create(
    B3Propagator.injectingMultiHeaders()
);
```

**Why B3?**
- Compatible with Temporal workflow workers
- Widely supported in distributed systems
- Works with Zipkin and Jaeger
- Supports multi-header format for better compatibility

### 5. Trace Context in Logs

**Problem**: Logs didn't include trace and span IDs, making correlation difficult.

**Solution**: Updated console log pattern to include trace context from MDC.

**Configuration**:
```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [trace_id=%X{trace_id}, span_id=%X{span_id}] %-5level %logger{36} - %msg%n</pattern>
```

## Dependencies Added

### Maven Dependencies (`pom.xml`)

1. **OpenTelemetry Logback Appender**:
   ```xml
   <dependency>
       <groupId>io.opentelemetry.instrumentation</groupId>
       <artifactId>opentelemetry-logback-appender-1.0</artifactId>
       <version>2.0.0</version>
   </dependency>
   ```

2. **OpenTelemetry Context Propagation**:
   ```xml
   <dependency>
       <groupId>io.opentelemetry</groupId>
       <artifactId>opentelemetry-context</artifactId>
       <version>1.34.1</version>
   </dependency>
   ```

3. **OpenTelemetry SDK Trace**:
   ```xml
   <dependency>
       <groupId>io.opentelemetry</groupId>
       <artifactId>opentelemetry-sdk-trace</artifactId>
       <version>1.34.1</version>
   </dependency>
   ```

4. **B3 Propagator**:
   ```xml
   <dependency>
       <groupId>io.opentelemetry</groupId>
       <artifactId>opentelemetry-extension-trace-propagators</artifactId>
       <version>1.34.1</version>
   </dependency>
   ```

## How It Works in Distributed Environments



1. **Context Propagation**: When workflow or activity is executed, the B3 propagator automatically:
   - Extracts trace context from incoming headers
   - Injects trace context into outgoing calls
   - Maintains trace continuity across workflow boundaries

2. **Log Correlation**: All logs within a workflow execution include the same `trace_id`, making it easy to:
   - Track workflow execution across multiple workers
   - Correlate logs from different services
   - Debug distributed workflows

3. **Span Creation**: Automatic span creation for:
   - Workflow executions
   - Activity executions
   - Service-to-service calls

## Configuration

### OTLP Endpoint

Configure the OTLP endpoint in `application.yaml`:

```yaml
otel:
  exporter:
    otlp:
      endpoint: http://your-collector:4318
```

For Temporal environments, ensure the endpoint is accessible from all workers.

### Environment Variables

Alternatively, use environment variables:

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://your-collector:4318
export OTEL_SERVICE_NAME=your-service-name
```

## Verification

### 1. Check Logs for Trace Context

Look for trace and span IDs in console output:

```
2026-01-09 15:30:00.000 [main] [trace_id=abc123, span_id=def456] INFO  c.e.d.DemoApplication - Starting application
```

### 2. Verify OTLP Export

Check your OTLP collector (e.g., Jaeger, Grafana Tempo) for:
- Traces with multiple spans
- Logs correlated with traces
- Service topology showing distributed calls

### 3. Test Distributed Tracing

```bash
# Make a request
curl http://localhost:8080/hello?name=Test

# Check logs for trace_id
# Verify the same trace_id appears in:
# - Service logs
# - Temporal worker logs
# - OTLP collector
```

## Troubleshooting

### Logs Not Appearing in OTLP Collector

1. **Check OTLP endpoint**: Ensure it's accessible from the application
2. **Verify network**: Check firewall rules and network connectivity
3. **Check logs**: Look for OpenTelemetry SDK errors in console

### Trace Context Not Propagating

1. **Verify B3 headers**: Check that B3 headers are present in HTTP requests
2. **Check propagator configuration**: Ensure B3 propagator is configured
3. **Global instance**: Verify OpenTelemetry is registered globally

### Missing Trace IDs in Logs

1. **Check Logback configuration**: Ensure OpenTelemetryAppender is configured
2. **Verify MDC**: Check that trace context is being set in MDC
3. **Log pattern**: Ensure console pattern includes `%X{trace_id}` and `%X{span_id}`

## Best Practices

1. **Always use the global OpenTelemetry instance** for context propagation
2. **Configure B3 propagator** for maximum compatibility
3. **Include trace context in all logs** for easy correlation
4. **Use batch processors** to reduce overhead
5. **Set appropriate resource attributes** for service identification
6. **Monitor OTLP export metrics** to ensure data is being sent

## Additional Resources

- [OpenTelemetry Java Documentation](https://opentelemetry.io/docs/instrumentation/java/)
- [Temporal OpenTelemetry Integration](https://docs.temporal.io/dev-guide/java/observability)
- [B3 Propagation Specification](https://github.com/openzipkin/b3-propagation)
- [OTLP Specification](https://opentelemetry.io/docs/specs/otlp/)
