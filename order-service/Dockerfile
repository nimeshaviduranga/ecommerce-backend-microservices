# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/app

# Copy maven executable and configuration
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY order-service/pom.xml order-service/

# Build dependencies layer
RUN ./mvnw dependency:go-offline -pl order-service

# Copy source and build the application
COPY order-service/src order-service/src
RUN ./mvnw package -DskipTests -pl order-service

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add a non-root user for better security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built JAR from the build stage
COPY --from=build /workspace/app/order-service/target/*.jar app.jar

# Switch to non-root user
USER spring:spring

# Set environment variables
ENV JAVA_OPTS="-Xms512m -Xmx1g"
ENV SPRING_PROFILES_ACTIVE="docker"

# Environment variables for service connections (override these when running)
ENV SERVICE_CART_SERVICE_URL="http://cart-service:9003"
ENV SERVICE_PRODUCT_SERVICE_URL="http://product-service:9001"
ENV SERVICE_USER_SERVICE_URL="http://user-service:9002"
ENV PAYMENT_STRIPE_KEY=""
ENV PAYMENT_STRIPE_ENABLED="false"

# Expose Order Service port
EXPOSE 9004

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]