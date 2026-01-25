FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy parent POM and backend POM
COPY ../../pom.xml /app/parent/pom.xml
COPY ../pom.xml /app/backend/pom.xml

# Copy service POM
COPY pom.xml /app/analytics-service/pom.xml

# Download dependencies
WORKDIR /app/analytics-service
RUN mvn dependency:go-offline -B

# Copy source code
COPY src /app/analytics-service/src

# Build
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/analytics-service/target/*.jar app.jar

# Expose port
EXPOSE 8090

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8090/api/analytics/health || exit 1

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]
