# Multi-stage build for optimized Micronaut application
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first (for layer caching)
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src
COPY aot-jar.properties aot-native-image.properties micronaut-cli.yml ./

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Final stage - runtime image
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 micronaut && \
    adduser -D -u 1001 -G micronaut micronaut

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/anonymouswall-*.jar app.jar

# Create log directory with proper permissions
# Directory: 755 (rwxr-xr-x) - owner can write, others can read/execute
# Files will be created with 640 (rw-r-----) permissions by log4j2.xml config
RUN mkdir -p /app/logs && \
    chmod 755 /app && \
    chmod 755 /app/logs && \
    chown -R micronaut:micronaut /app

# Switch to non-root user
USER micronaut

# Expose port 8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
