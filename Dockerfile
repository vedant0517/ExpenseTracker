# ==============================================================================
# Multi-Stage Production Dockerfile for Personal Expense Tracker REST API
# ==============================================================================

# Stage 1: Dependency resolution and compilation
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Lightweight, secure JRE runtime container
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as non-root user for security best practices
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

COPY --from=builder /app/target/personal-expense-tracker-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
