# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app

# Copy maven executable and configuration
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY service-registry/pom.xml service-registry/

# Build dependencies layer
RUN ./mvnw dependency:go-offline -pl service-registry

# Copy source and build the application
COPY service-registry/src service-registry/src
RUN ./mvnw package -DskipTests -pl service-registry

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add a non-root user for better security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built JAR from the build stage
COPY --from=build /workspace/app/service-registry/target/*.jar app.jar

# Switch to non-root user
USER spring:spring

# Set environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE="docker"

# Eureka configuration environment variables
ENV EUREKA_CLIENT_REGISTER_WITH_EUREKA="false"
ENV EUREKA_CLIENT_FETCH_REGISTRY="false"
ENV EUREKA_SERVER_ENABLE_SELF_PRESERVATION="true"
ENV EUREKA_SERVER_RENEWAL_PERCENT_THRESHOLD="0.85"

# Expose Eureka Server port
EXPOSE 8761

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]