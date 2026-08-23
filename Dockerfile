# ==============================================================================
# Taxoryn Platform - Production Multi-Stage Dockerfile
# Modular Monolith Multi-Tenant Practice Management SaaS
# ==============================================================================

# --- Stage 1: Builder ---
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Stage 2: Production Runtime ---
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="Taxoryn Engineering <engineering@taxoryn.com>"
LABEL description="Taxoryn Enterprise Tax Practice Management Platform"

# Install curl for healthcheck
RUN apk add --no-cache curl tzdata

# Create dedicated non-root application user and directories
RUN addgroup -g 10001 -S taxoryn && \
    adduser -u 10001 -S taxoryn -G taxoryn && \
    mkdir -p /app /app/data/documents /app/logs && \
    chown -R taxoryn:taxoryn /app

WORKDIR /app

# Copy built artifact from builder stage
COPY --from=builder --chown=taxoryn:taxoryn /build/target/taxoryn-core-*.jar /app/taxoryn.jar

USER taxoryn:taxoryn

# Set JVM and Application Environment Defaults
ENV SPRING_PROFILES_ACTIVE=demo \
    SERVER_PORT=8088 \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Dfile.encoding=UTF-8"

EXPOSE 8088

# Healthcheck monitoring
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -f http://localhost:8088/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/taxoryn.jar"]
