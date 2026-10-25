# ==============================================================================
# Multi-stage Dockerfile for Chemical Storage Monitor
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build the Spring Boot application using Eclipse Temurin JDK 26
# ------------------------------------------------------------------------------
FROM eclipse-temurin:26-jdk AS builder

WORKDIR /workspace

# Copy Maven wrapper and descriptor to leverage Docker caching for dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure correct LF line endings and executable permissions for Maven wrapper
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Resolve project dependencies (speeds up subsequent builds)
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and build executable JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ------------------------------------------------------------------------------
# Stage 2: Production-ready runtime image using Eclipse Temurin JRE 26
# ------------------------------------------------------------------------------
FROM eclipse-temurin:26-jre

WORKDIR /app

# Create a dedicated non-root user and group for security
RUN groupadd -r spring && useradd -r -g spring spring

# Copy built JAR artifact from builder stage
COPY --from=builder /workspace/target/chemical-storage-monitor-*.jar app.jar

# Set ownership to non-root user
RUN chown spring:spring /app/app.jar

USER spring:spring

# Expose Spring Boot HTTP port
EXPOSE 8080

# Configure JVM flags optimized for container environments
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
