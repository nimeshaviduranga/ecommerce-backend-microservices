# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app

# Copy maven executable and configuration
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY config-server/pom.xml config-server/

# Build dependencies layer
RUN ./mvnw dependency:go-offline -pl config-server

# Copy source and build the application
COPY config-server/src config-server/src
RUN ./mvnw package -DskipTests -pl config-server

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add a non-root user for better security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built JAR from the build stage
COPY --from=build /workspace/app/config-server/target/*.jar app.jar

# Create directory for local config files if using native profile
RUN mkdir config-repo && chown spring:spring config-repo

# Switch to non-root user
USER spring:spring

# Set environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE="docker"

# Expose Config Server port
EXPOSE 8888

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]