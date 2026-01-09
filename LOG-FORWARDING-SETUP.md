# OpenTelemetry Log Forwarding Setup for Distributed Environments

## Overview

This document describes the OpenTelemetry instrumentation setup for forwarding logs to an OTLP endpoint in distributed environments, including worker processes, microservices, and containerized applications.

## Problem Statement

The original implementation was not working in distributed environments because:
1. Logs were only going to console, not being forwarded to the OTLP endpoint
2. The OpenTelemetry SDK was not registered globally, preventing the Logback appender from accessing it
3. Missing the OpenTelemetry Logback appender to bridge SLF4J logs to OpenTelemetry

## Solution: Log Forwarding Only

This implementation focuses solely on forwarding logs to an OTLP endpoint without trace instrumentation.

### Key Changes

#### 1. OpenTelemetry Logback Appender

**Added**: `OpenTelemetryAppender` to Logback configuration to automatically bridge SLF4J logs to OpenTelemetry.

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

**What it captures**:
- Log level, message, and timestamp
- Logger name and thread information
- Code location (class, method, line number)
- MDC (Mapped Diagnostic Context) attributes
- Exception stack traces
- Custom key-value pairs

#### 2. Global OpenTelemetry Instance

**Problem**: The Logback appender needs access to the OpenTelemetry SDK to forward logs.

**Solution**: Register the OpenTelemetry SDK globally using `buildAndRegisterGlobal()`.

**Code** (`DemoApplication.java`):
```java
openTelemetrySdk = OpenTelemetrySdk.builder()
    .setLoggerProvider(sdkLoggerProvider)
    .buildAndRegisterGlobal();
```

This ensures:
- The Logback appender can access the SDK from any thread
- Works across multiple workers in distributed systems
- Logs are forwarded even from background threads

#### 3. OTLP Log Exporter Configuration

**Configuration**:
```java
OtlpGrpcLogRecordExporter logExporter = OtlpGrpcLogRecordExporter.builder()
    .setEndpoint(otlpEndpoint)
    .build();

SdkLoggerProvider sdkLoggerProvider = SdkLoggerProvider.builder()
    .setResource(resource)
    .addLogRecordProcessor(BatchLogRecordProcessor.builder(logExporter).build())
    .build();
```

**Features**:
- Batch processing for efficiency
- Automatic retry on failure
- Resource attributes for service identification
- gRPC protocol for reliable delivery

## Dependencies Added

### Maven Dependencies (`pom.xml`)

**OpenTelemetry Logback Appender**:
```xml
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-logback-appender-1.0</artifactId>
    <version>2.1.0-alpha</version>
</dependency>
```

This is the only additional dependency needed for log forwarding.

## How It Works in Distributed Environments

### Architecture

```
Application Logs (SLF4J)
        ↓
Logback Framework
        ↓
OpenTelemetry Appender
        ↓
Global OpenTelemetry SDK
        ↓
OTLP Log Exporter (Batch)
        ↓
OTLP Endpoint (Collector)
```

### Distributed Workers

1. **Multiple Instances**: Each worker/service instance has its own OpenTelemetry SDK instance
2. **Independent Forwarding**: Logs from each instance are forwarded independently to the OTLP endpoint
3. **Service Identification**: Resource attributes identify which service/instance generated the logs

### Example Flow

```
Worker Instance 1 → Logs → OTLP Endpoint
Worker Instance 2 → Logs → OTLP Endpoint
Worker Instance 3 → Logs → OTLP Endpoint
```

All logs include:
- Service name
- Service version
- Deployment environment
- Logger name
- Log level
- Timestamp
- Message
- Code location

## Configuration

### OTLP Endpoint

Configure the OTLP endpoint in `application.yaml`:

```yaml
otel:
  service:
    name: your-service-name
  exporter:
    otlp:
      endpoint: http://your-collector:4318
```

### Environment Variables

Alternatively, use environment variables:

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://your-collector:4318
export OTEL_SERVICE_NAME=your-service-name
```

### For Distributed Workers

Ensure the OTLP endpoint is accessible from all worker instances:
- Use a centralized collector endpoint
- Configure network access for all workers
- Consider using a sidecar collector pattern for better reliability

## Verification

### 1. Check Console Logs

Verify logs are being generated:

```
2026-01-09 15:30:00.000 [main] INFO  c.e.d.DemoApplication - Starting application
```

### 2. Verify OTLP Export

Check your OTLP collector (e.g., Grafana Loki, Elasticsearch) for:
- Logs appearing from the service
- Correct service name in resource attributes
- All log levels being captured
- Code location information

### 3. Test Log Forwarding

```bash
# Start the application
mvn spring-boot:run

# Generate logs
curl http://localhost:8080/test-logs

# Check your OTLP collector for the logs
```

## Troubleshooting

### Logs Not Appearing in OTLP Collector

**Check OTLP endpoint**:
```bash
# Test connectivity
curl -v http://your-collector:4318/v1/logs
```

**Verify configuration**:
- Ensure `otel.exporter.otlp.endpoint` is correct
- Check firewall rules
- Verify network connectivity from all workers

**Check application logs**:
```
# Look for OpenTelemetry errors
grep -i "opentelemetry" application.log
```

### Logs Only in Console, Not Forwarded

**Verify Logback configuration**:
- Ensure `OTEL` appender is referenced in logger configuration
- Check that OpenTelemetryAppender class is on classpath

**Check global registration**:
```java
// Should see this log message
"OpenTelemetry SDK configured successfully!"
"Log forwarding enabled to OTLP endpoint"
```

### Performance Issues

**Adjust batch processor settings**:
```java
BatchLogRecordProcessor.builder(logExporter)
    .setMaxQueueSize(2048)
    .setMaxExportBatchSize(512)
    .setScheduleDelay(Duration.ofSeconds(5))
    .build()
```

**Monitor memory usage**:
- Batch processor queues logs in memory
- Adjust queue size based on log volume
- Consider export delay vs. memory trade-off

## Best Practices

### 1. Use Structured Logging

```java
logger.info("User action completed", 
    kv("userId", userId),
    kv("action", "purchase"),
    kv("amount", amount));
```

### 2. Set Appropriate Log Levels

- **ERROR**: Errors requiring immediate attention
- **WARN**: Potential issues
- **INFO**: Important business events
- **DEBUG**: Detailed diagnostic information

### 3. Include Context in MDC

```java
MDC.put("requestId", requestId);
MDC.put("userId", userId);
try {
    // Your code
} finally {
    MDC.clear();
}
```

### 4. Configure Resource Attributes

```java
Resource resource = Resource.create(Attributes.builder()
    .put(ResourceAttributes.SERVICE_NAME, serviceName)
    .put(ResourceAttributes.SERVICE_VERSION, version)
    .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, environment)
    .put(ResourceAttributes.SERVICE_INSTANCE_ID, instanceId)
    .build());
```

### 5. Monitor Export Metrics

- Track log export success/failure rates
- Monitor batch processor queue size
- Alert on export failures

## Distributed Environment Considerations

### Worker Processes

1. **Instance Identification**: Use unique service instance IDs
2. **Centralized Collection**: Point all workers to the same collector
3. **Network Reliability**: Consider retry policies and timeouts
4. **Resource Limits**: Configure appropriate batch sizes for worker resources

### Kubernetes/OpenShift

1. **Sidecar Pattern**: Deploy OTLP collector as sidecar for reliability
2. **Service Mesh**: Use service mesh for secure communication
3. **ConfigMaps**: Externalize OTLP endpoint configuration
4. **Health Checks**: Monitor collector availability

### High-Volume Scenarios

1. **Sampling**: Consider log sampling for very high volumes
2. **Filtering**: Filter out noisy logs at the appender level
3. **Compression**: Enable gRPC compression
4. **Load Balancing**: Use multiple collector instances

## Additional Resources

- [OpenTelemetry Java Documentation](https://opentelemetry.io/docs/instrumentation/java/)
- [OpenTelemetry Logback Appender](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/logback/logback-appender-1.0)
- [OTLP Specification](https://opentelemetry.io/docs/specs/otlp/)