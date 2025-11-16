MSB-Preparation — Spring Boot Microservices Architecture

This repository contains a complete Spring Boot Microservices setup including:

Service Discovery (Eureka Server & Eureka Clients)

API Gateway (Spring Cloud Gateway)

Feign Client communication

JWT Authentication

H2 Databases

Load-balanced service calls through Eureka

Spring Boot 3.2.x + Spring Cloud 2023.x

This project is designed for hands-on practice of real microservices architecture.

Project Structure
MSB-preparation/
│
├── EurekaServer/               # Eureka discovery server (8761)
├── api-gateway/                # API Gateway (8080)
│
├── orderservice/               # Order microservice (9003)
│      └── Uses Feign client to call product-service
│
├── productservice/             # Product microservice (9002)
│
├── user-service/               # Authentication service (JWT) (9001)
│
└── README.md

Microservice Ports
Service	Port
Eureka Server	8761
API Gateway	8080
user-service	9001
productservice	9002
orderservice	9003
Microservice Overview
1. Eureka Server

Handles service registration and discovery

Dashboard available at:

http://localhost:8761

2. API Gateway

Single entry point for all requests

Performs routing using Eureka load balancer

Runs on port 8080

3. User Service

Handles:

User registration

Login

JWT token generation

All endpoints prefixed with:

/auth/**

4. Product Service

Provides:

List products

Get product by ID

Reduce product stock

5. Order Service

Handles:

Create order

Get order details

Communicates with product-service using Feign:

order-service → product-service

API Gateway Routes
Path Pattern	Routed To
/api/orders/**	order-service
/api/products/**	product-service
/auth/**	user-service

Internal routing example:

uri: lb://order-service

How to Run the Project
Step 1 — Start Eureka Server
cd EurekaServer
mvn spring-boot:run

Step 2 — Start Microservices
cd user-service && mvn spring-boot:run
cd productservice && mvn spring-boot:run
cd orderservice && mvn spring-boot:run

Step 3 — Start API Gateway
cd api-gateway
mvn spring-boot:run

Example API Endpoints (through Gateway)
User Service
POST http://localhost:8080/auth/register
POST http://localhost:8080/auth/login

Product Service
GET  http://localhost:8080/api/products
GET  http://localhost:8080/api/products/{id}
POST http://localhost:8080/api/products/{id}/reduce?qty=1

Order Service
POST http://localhost:8080/api/orders
GET  http://localhost:8080/api/orders/{id}


Future Enhancements

Add Resilience4j (Circuit Breaker, Retry)

Add Distributed Tracing (Zipkin)

Add Spring Cloud Config Server

Add Rate Limiting at Gateway

Add Kafka for asynchronous communication

Add Docker Compose for full environment setup
