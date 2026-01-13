# OpenTelemetry Trace Forwarding Setup

## Overview

This document describes the complete setup for forwarding both **logs** and **traces** to an OTLP (OpenTelemetry Protocol) endpoint. The application now supports full distributed tracing capabilities with context propagation.

## What Was Enabled

### 1. Trace Exporter Configuration

**Added**: OTLP gRPC Span Exporter for sending traces to the OTLP endpoint.

**Code** (`DemoApplication.java`):
```java
// Configure OTLP Span Exporter for traces
OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
    .setEndpoint(otlpEndpoint)
    .build();
```

### 2. Tracer Provider

**Added**: `SdkTracerProvider` to create and manage trace spans.

**Code**:
```java
// Create SdkTracerProvider with batch processor
SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
    .setResource(resource)
    .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
    .build();
```

**Benefits**:
- Automatic span creation for operations
- Batch processing for efficient export
- Resource attributes attached to all spans

### 3. B3 Context Propagation

**Added**: B3 propagator for distributed tracing across service boundaries.

**Code**:
```java
// Configure B3 propagator for distributed tracing compatibility
ContextPropagators contextPropagators = ContextPropagators.create(
    B3Propagator.injectingMultiHeaders()
);
```

**Why B3?**
- Compatible with Temporal workflow workers
- Widely supported in distributed systems (Zipkin, Jaeger)
- Multi-header format for better compatibility
- Industry standard for trace context propagation

### 4. Global OpenTelemetry Registration

**Updated**: OpenTelemetry SDK now includes tracer provider and propagators.

**Code**:
```java
openTelemetrySdk = OpenTelemetrySdk.builder()
    .setLoggerProvider(sdkLoggerProvider)
    .setTracerProvider(sdkTracerProvider)
    .setPropagators(contextPropagators)
    .buildAndRegisterGlobal();
```

**Benefits**:
- Global instance enables context propagation across threads
- Logback appender can access trace context
- Automatic trace context injection in logs

### 5. Trace Context in Logs

**Updated**: Console log pattern now includes trace_id and span_id.

**Configuration** (`logback-spring.xml`):
```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [trace_id=%X{trace_id}, span_id=%X{span_id}] %-5level %logger{36} - %msg%n</pattern>
```

**Example Output**:
```
2026-01-13 15:30:00.000 [main] [trace_id=abc123def456, span_id=789xyz] INFO  c.e.d.DemoApplication - Starting application
```

### 6. Application Configuration

**Updated**: `application.yaml` now includes trace exporter configuration.

**Configuration**:
```yaml
otel:
  logs:
    exporter: otlp
  traces:
    exporter: otlp
```

## Dependencies Added

The following Maven dependencies were added to `pom.xml`:

### 1. OpenTelemetry SDK Trace
```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk-trace</artifactId>
    <version>${opentelemetry.version}</version>
</dependency>
```

### 2. OpenTelemetry Context Propagation
```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-context</artifactId>
    <version>${opentelemetry.version}</version>
</dependency>
```

### 3. B3 Propagator
```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-extension-trace-propagators</artifactId>
    <version>${opentelemetry.version}</version>
</dependency>
```

## How It Works

### Data Flow

```
Application Code
    ↓
OpenTelemetry SDK
    ↓
┌─────────────────┬─────────────────┐
│   Log Records   │   Trace Spans   │
└────────┬────────┴────────┬────────┘
         ↓                 ↓
    OTLP Exporter    OTLP Exporter
         ↓                 ↓
    ┌────────────────────────┐
    │   OTLP Collector       │
    │  (Jaeger/Tempo/etc)    │
    └────────────────────────┘
```

### Distributed Tracing Flow

```
Service A → Service B → Service C
    |           |           |
trace_id=123  trace_id=123  trace_id=123
span_id=001   span_id=002   span_id=003
    |           |           |
    └───────────┴───────────┘
            ↓
    Correlated in OTLP Backend
```

## Configuration

### OTLP Endpoint

Configure the OTLP endpoint in `application.yaml`:

```yaml
otel:
  exporter:
    otlp:
      endpoint: http://your-collector:4318
      protocol: http/protobuf
```

Or use environment variables:

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://your-collector:4318
export OTEL_SERVICE_NAME=spring-boot-otel-demo
```

### Supported Protocols

- **gRPC**: `http://collector:4317` (default in code)
- **HTTP/Protobuf**: `http://collector:4318` (configured in yaml)

## Testing

### 1. Start the Application

```bash
cd spring-boot-otel-demo
mvn spring-boot:run
```

### 2. Make a Request

```bash
curl http://localhost:8080/hello?name=Test
```

### 3. Check Console Output

Look for trace context in logs:

```
2026-01-13 15:30:00.000 [http-nio-8080-exec-1] [trace_id=abc123, span_id=def456] INFO  c.e.d.c.HelloController - Received request
```

### 4. Verify in OTLP Backend

Check your OTLP collector (Jaeger, Grafana Tempo, etc.) for:
- **Traces**: Complete trace with spans
- **Logs**: Logs correlated with traces via trace_id
- **Service Map**: Visual representation of service dependencies

## Verification Checklist

- [ ] Application starts without errors
- [ ] Console logs include trace_id and span_id
- [ ] Traces appear in OTLP backend
- [ ] Logs appear in OTLP backend
- [ ] Logs are correlated with traces (same trace_id)
- [ ] B3 headers are propagated in HTTP requests

## Troubleshooting

### Traces Not Appearing

1. **Check OTLP endpoint**: Ensure it's accessible
   ```bash
   curl http://your-collector:4318/v1/traces
   ```

2. **Verify configuration**: Check `application.yaml` settings

3. **Check logs**: Look for OpenTelemetry SDK errors

### Trace Context Not in Logs

1. **Verify Logback configuration**: Ensure pattern includes `%X{trace_id}` and `%X{span_id}`

2. **Check OpenTelemetry registration**: Ensure SDK is registered globally

3. **Verify OpenTelemetryAppender**: Ensure it's configured in logback-spring.xml

### Context Not Propagating

1. **Check B3 headers**: Verify headers in HTTP requests
   ```
   X-B3-TraceId: <trace_id>
   X-B3-SpanId: <span_id>
   X-B3-Sampled: 1
   ```

2. **Verify propagator**: Ensure B3Propagator is configured

3. **Check global instance**: Ensure OpenTelemetry is registered globally

## Best Practices

1. **Use Batch Processors**: Reduces overhead by batching exports
2. **Configure Resource Attributes**: Helps identify services in distributed systems
3. **Enable B3 Propagation**: Ensures compatibility with most systems
4. **Include Trace Context in Logs**: Makes debugging easier
5. **Monitor Export Metrics**: Track success/failure of exports
6. **Set Appropriate Sampling**: Balance between observability and performance

## Integration with Distributed Systems

### Temporal Workflows

The B3 propagator ensures trace context is automatically propagated through Temporal workflows:

```java
// Trace context is automatically propagated
WorkflowClient client = WorkflowClient.newInstance(service);
MyWorkflow workflow = client.newWorkflowStub(MyWorkflow.class);
workflow.execute(); // Trace context flows through workflow
```

### Microservices

When calling other services, trace context is automatically injected:

```java
// RestTemplate or WebClient automatically includes B3 headers
restTemplate.getForObject("http://other-service/api", String.class);
```

## Additional Resources

- [OpenTelemetry Java Documentation](https://opentelemetry.io/docs/instrumentation/java/)
- [OTLP Specification](https://opentelemetry.io/docs/specs/otlp/)
- [B3 Propagation Specification](https://github.com/openzipkin/b3-propagation)
- [Distributed Tracing Best Practices](https://opentelemetry.io/docs/concepts/signals/traces/)

## Summary

The application now supports:
- ✅ Log forwarding to OTLP
- ✅ Trace forwarding to OTLP
- ✅ B3 context propagation
- ✅ Trace context in logs
- ✅ Distributed tracing support
- ✅ Temporal workflow compatibility