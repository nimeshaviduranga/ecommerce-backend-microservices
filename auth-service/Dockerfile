# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app

# Copy maven executable and configuration
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY auth-service/pom.xml auth-service/

# Build dependencies layer (this will be cached if dependencies don't change)
RUN ./mvnw dependency:go-offline -pl auth-service

# Copy source and build the application
COPY auth-service/src auth-service/src
RUN ./mvnw package -DskipTests -pl auth-service

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add a non-root user to run the application
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built JAR from the build stage
COPY --from=build /workspace/app/auth-service/target/*.jar app.jar

# Set environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE="docker"

# Expose the port the app runs on
EXPOSE 9001

# Run the application with security and performance parameters
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]