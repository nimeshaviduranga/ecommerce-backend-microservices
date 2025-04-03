# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app

# Copy maven executable and configuration
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY notification-service/pom.xml notification-service/

# Build dependencies layer
RUN ./mvnw dependency:go-offline -pl notification-service

# Copy source and build the application
COPY notification-service/src notification-service/src
RUN ./mvnw package -DskipTests -pl notification-service

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add a non-root user for better security
RUN addgroup -S spring && adduser -S spring -G spring

# Create directories for templates and credentials
RUN mkdir -p templates/email templates/sms config && chown -R spring:spring templates config

# Copy the built JAR from the build stage
COPY --from=build /workspace/app/notification-service/target/*.jar app.jar

# Copy email and SMS templates if they exist externally
# COPY notification-service/src/main/resources/templates/email/ templates/email/
# COPY notification-service/src/main/resources/templates/sms/ templates/sms/

# Make a directory for Firebase credentials and ensure proper permissions
RUN mkdir -p /app/config/firebase && chown -R spring:spring /app/config

# Switch to non-root user
USER spring:spring

# Set environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE="docker"

# Additional environment variables for notification services
ENV MAIL_USERNAME=""
ENV MAIL_PASSWORD=""
ENV TWILIO_ACCOUNT_SID=""
ENV TWILIO_AUTH_TOKEN=""
ENV TWILIO_PHONE_NUMBER=""

# Expose Notification Service port
EXPOSE 9006

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]