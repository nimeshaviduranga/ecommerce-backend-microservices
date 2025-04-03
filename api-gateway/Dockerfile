FROM eclipse-temurin:21-jdk-alpine as build
WORKDIR /workspace/app

# Copy maven files first for better layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY [service-name]/pom.xml [service-name]/

# Download dependencies (this layer can be cached)
RUN ./mvnw dependency:go-offline -pl api-gateway

# Copy source code
COPY [service-name]/src [service-name]/src

# Build application
RUN ./mvnw package -DskipTests -pl api-gateway

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/app/[service-name]/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]