# E-Commerce Backend Microservices

A comprehensive microservices-based backend system for e-commerce applications.

## Services

- **API Gateway**: Entry point for all client requests
- **Config Server**: Centralized configuration management
- **Service Registry**: Service discovery using Eureka
- **Auth Service**: Authentication and authorization
- **User Service**: User management and profiles
- **Product Service**: Product catalog and inventory
- **Order Service**: Order processing and management
- **Payment Service**: Payment processing
- **Cart Service**: Shopping cart management
- **Notification Service**: Email, SMS, and push notifications

## Technology Stack

- **Java 21**
- **Spring Boot 3.2.0**
- **Spring Cloud 2023.0.0**
- **Spring Security with JWT**
- **Spring Data JPA**
- **PostgreSQL**
- **Kafka**
- **Docker & Kubernetes**

## Getting Started

### Prerequisites

- Java 21
- Maven 3.6 or higher
- PostgreSQL
- Kafka (for event-driven services)
- Docker (optional, for containerization)

### Building the Project

To build all microservices:

```bash
mvn clean install
```

### Running the Services

The services should be started in a specific order:

1. Config Server
2. Service Registry
3. Other services

#### Start Config Server:

```bash
cd config-server
mvn spring-boot:run
```

#### Start Service Registry:

```bash
cd service-registry
mvn spring-boot:run
```

#### Start Other Services:

```bash
cd 
mvn spring-boot:run
```
## Development Guidelines

- Each microservice follows a similar package structure
- Use Spring Security for authentication and authorization
- Follow REST API best practices
- Use Spring Cloud Config for centralized configuration
- Implement circuit breakers for resilience

## Docker Support

Each microservice includes a Dockerfile for containerization. To build and run with Docker:

```bash
# Build docker image
docker build -t ecommerce/ /

# Run container
docker run -p : ecommerce/
```

## Kubernetes Deployment

Kubernetes manifests are provided in the `kubernetes/` directory for each service.
