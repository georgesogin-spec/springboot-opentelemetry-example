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

## 🔨 Building the Project

```bash
cd spring-boot-otel-demo
mvn clean package
```

## ▶️ Running the Application

```bash
java -jar target/spring-boot-otel-demo-1.0.0.jar
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

**Happy Logging! 🎉**
