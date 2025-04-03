# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app

# Copy maven executable and configuration
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY payment-service/pom.xml payment-service/

# Build dependencies layer
RUN ./mvnw dependency:go-offline -pl payment-service

# Copy source and build the application
COPY payment-service/src payment-service/src
RUN ./mvnw package -DskipTests -pl payment-service

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add a non-root user for better security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built JAR from the build stage
COPY --from=build /workspace/app/payment-service/target/*.jar app.jar

# Switch to non-root user
USER spring:spring

# Set environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE="docker"

# Environment variables for payment processing
ENV STRIPE_API_KEY=""
ENV STRIPE_WEBHOOK_SECRET=""
ENV PAYMENT_SANDBOX_MODE="true"
ENV SERVICE_ORDER_SERVICE_URL="http://order-service:9004"

# Expose Payment Service port
EXPOSE 9005

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]