# Spring Boot OpenTelemetry Demo

A Spring Boot application demonstrating OpenTelemetry integration for sending logs to an OTLP (OpenTelemetry Protocol) endpoint.

## 📋 Overview

This project showcases:
- ✅ Spring Boot 3.2.1 with Java 17
- ✅ OpenTelemetry SDK integration
- ✅ Automatic log export to OTLP endpoint
- ✅ Logback with OpenTelemetry appender
- ✅ RESTful endpoints for testing
- ✅ Configurable OTLP endpoint via YAML

## 🏗️ Project Structure

```
spring-boot-otel-demo/
├── pom.xml                                    # Maven configuration
├── README.md                                  # This file
└── src/
    └── main/
        ├── java/
        │   └── com/example/demo/
        │       ├── DemoApplication.java       # Main application class
        │       └── controller/
        │           └── HelloController.java   # REST controller
        └── resources/
            ├── application.yaml               # Application configuration
            └── logback-spring.xml             # Logging configuration
```

## 🚀 Prerequisites

- Java 17 or higher
- Maven 3.6+
- An OTLP-compatible collector (optional for testing)

## ⚙️ Configuration

### OTLP Endpoint Configuration

Edit `src/main/resources/application.yaml` to configure your OTLP endpoint:

```yaml
otel:
  exporter:
    otlp:
      endpoint: http://localhost:4318  # Change this to your OTLP endpoint
      protocol: http/protobuf          # or 'grpc'
```

### Supported Endpoints

- **Local HTTP**: `http://localhost:4318`
- **Local gRPC**: `http://localhost:4317`
- **Cloud Provider**: Update with your provider's endpoint

### Environment Variables (Alternative Configuration)

You can also configure via environment variables:

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
export OTEL_SERVICE_NAME=spring-boot-otel-demo
```

## 🔨 Building the Project

```bash
cd spring-boot-otel-demo
mvn clean package
```

## ▶️ Running the Application

### Option 1: Using Maven

```bash
mvn spring-boot:run
```

### Option 2: Using Java

```bash
java -jar target/spring-boot-otel-demo-1.0.0.jar
```

### Option 3: With Custom OTLP Endpoint

```bash
java -Dotel.exporter.otlp.endpoint=http://your-endpoint:4318 \
     -jar target/spring-boot-otel-demo-1.0.0.jar
```

## 🧪 Testing the Application

### 1. Check Application Status

```bash
curl http://localhost:8080/
```

**Expected Response:**
```json
{
  "status": "running",
  "message": "Spring Boot OpenTelemetry Demo Application",
  "endpoints": "/hello, /health"
}
```

### 2. Test Hello Endpoint

```bash
curl http://localhost:8080/hello
```

**Expected Response:**
```json
{
  "message": "Hello, World!",
  "timestamp": "2026-01-08T12:30:00.123",
  "service": "spring-boot-otel-demo"
}
```

**With custom name:**
```bash
curl "http://localhost:8080/hello?name=OpenTelemetry"
```

### 3. Test Log Levels

```bash
curl http://localhost:8080/test-logs
```

This endpoint generates logs at all levels (TRACE, DEBUG, INFO, WARN, ERROR) to test the OpenTelemetry log export.

### 4. Health Check

```bash
curl http://localhost:8080/actuator/health
```

## 📊 Verifying Log Export

### Console Output

When the application runs, you'll see logs in the console:

```
2026-01-08 12:30:00.123 [main] INFO  c.e.demo.DemoApplication - Starting Spring Boot OpenTelemetry Demo Application...
2026-01-08 12:30:00.456 [main] INFO  c.e.demo.DemoApplication - Initializing OpenTelemetry with OTLP endpoint: http://localhost:4318
2026-01-08 12:30:00.789 [main] INFO  c.e.demo.DemoApplication - OpenTelemetry SDK configured successfully!
```

### OTLP Endpoint

Logs are automatically exported to the configured OTLP endpoint. Check your collector/backend to verify:

1. **Jaeger**: http://localhost:16686 (if using Jaeger)
2. **Grafana**: Check your Loki datasource
3. **Cloud Provider**: Check your observability dashboard

## 🐳 Running with Local OTLP Collector (Optional)

### Using Jaeger (All-in-One)

```bash
docker run -d --name jaeger \
  -e COLLECTOR_OTLP_ENABLED=true \
  -p 16686:16686 \
  -p 4317:4317 \
  -p 4318:4318 \
  jaegertracing/all-in-one:latest
```

Then access Jaeger UI at: http://localhost:16686

### Using OpenTelemetry Collector

Create `otel-collector-config.yaml`:

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

exporters:
  logging:
    loglevel: debug

service:
  pipelines:
    logs:
      receivers: [otlp]
      exporters: [logging]
```

Run the collector:

```bash
docker run -d --name otel-collector \
  -p 4317:4317 \
  -p 4318:4318 \
  -v $(pwd)/otel-collector-config.yaml:/etc/otel-collector-config.yaml \
  otel/opentelemetry-collector:latest \
  --config=/etc/otel-collector-config.yaml
```

## 📝 Key Features

### OpenTelemetry Integration

- **Automatic Log Export**: All logs are automatically sent to the OTLP endpoint
- **Resource Attributes**: Service name, version, and environment are included
- **Batch Processing**: Logs are batched for efficient export
- **Graceful Shutdown**: Ensures all logs are flushed on application shutdown

### Logging Configuration

The application uses Logback with two appenders:
1. **CONSOLE**: For local development visibility
2. **OTEL**: For exporting logs to OTLP endpoint

### REST Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Application status |
| `/hello` | GET | Hello world with optional name parameter |
| `/test-logs` | GET | Generate logs at all levels |
| `/actuator/health` | GET | Health check endpoint |

## 🔧 Troubleshooting

### Logs Not Appearing in OTLP Endpoint

1. **Check endpoint configuration** in `application.yaml`
2. **Verify OTLP collector is running**: `curl http://localhost:4318/v1/logs`
3. **Check application logs** for connection errors
4. **Verify network connectivity** between app and collector

### Application Won't Start

1. **Check Java version**: `java -version` (should be 17+)
2. **Verify port 8080 is available**: `lsof -i :8080`
3. **Check Maven build**: `mvn clean package`

### Connection Refused Errors

If you see connection errors to the OTLP endpoint:
- The application will still run, but logs won't be exported
- Start your OTLP collector or update the endpoint configuration
- The application logs will show the connection attempts

## 📚 Dependencies

Key dependencies used in this project:

- **Spring Boot**: 3.2.1
- **OpenTelemetry SDK**: 1.34.1
- **OpenTelemetry Instrumentation**: 2.0.0
- **Java**: 17

## 🤝 Contributing

Feel free to submit issues and enhancement requests!

## 📄 License

This project is provided as-is for demonstration purposes.

## 🔗 Useful Links

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [OTLP Specification](https://opentelemetry.io/docs/specs/otlp/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)

---

**Happy Logging! 🎉**
