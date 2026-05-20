# Commerce API

A production-style e-commerce backend API built using Spring Boot, PostgreSQL, Docker, JWT authentication, and relational commerce architecture.

---

# Project Overview

Commerce API is a backend-focused e-commerce platform that implements:

- JWT Authentication
- User Registration/Login
- Product Management
- Cart Management
- Checkout System
- Order Persistence
- Inventory Reduction
- PostgreSQL Persistence
- Dockerized Deployment
- REST API Architecture
- Layered Backend Design

The project follows a modern backend architecture using:

```text
Controller
   ↓
Service / Business Logic
   ↓
Repository Layer
   ↓
PostgreSQL Database
```

All components are containerized using Docker Compose.

---

# Tech Stack

## Backend

- Java 21
- Spring Boot 3.3.5
- Spring Security
- Spring Data JPA
- Hibernate
- JWT Authentication
- Maven
- Lombok

## Database

- PostgreSQL 16

## Containerization

- Docker
- Docker Compose

## API Testing

- Postman

---

# Features Implemented

# Authentication System

## User Registration

Endpoint:

```http
POST /auth/register
```

Features:

- User creation
- Username uniqueness validation
- BCrypt password hashing
- Validation handling
- PostgreSQL persistence

Example Request:

```json
{
  "username": "sid",
  "email": "sid@example.com",
  "password": "mypassword"
}
```

---

## User Login

Endpoint:

```http
POST /auth/login
```

Features:

- Credential validation
- JWT token generation
- Stateless authentication

Example Request:

```json
{
  "username": "sid",
  "password": "mypassword"
}
```

Example Response:

```json
{
  "token": "eyJhbGc..."
}
```

---

## JWT Authentication

Implemented:

- JWT token generation
- JWT validation
- Stateless authentication
- Authorization filter
- Protected routes

Protected endpoints require:

```http
Authorization: Bearer YOUR_TOKEN
```

---

# Product System

## Product Entity

Fields:

```java
id
name
price
stockQty
description
```

---

## Product APIs

### Get All Products

```http
GET /products
```

---

### Get Product By ID

```http
GET /products/{id}
```

---

### Create Product

```http
POST /products
```

Example Request:

```json
{
  "name": "Protein Bar",
  "price": 120,
  "stockQty": 50,
  "description": "High protein snack bar"
}
```

---

### Update Product

```http
PUT /products/{id}
```

---

### Delete Product

```http
DELETE /products/{id}
```

---

# Cart System

The cart system supports:

- User-owned carts
- Add-to-cart
- Remove-from-cart
- Quantity increment/decrement
- Persistent cart storage

---

# Cart Architecture

```text
User
 ↳ Cart
      ↳ CartItems
           ↳ Product
```

---

## Cart Entity

Fields:

```java
id
user
createdAt
cartItems
```

Relationships:

- One cart belongs to one user
- One cart contains many cart items

---

## CartItem Entity

Fields:

```java
id
cart
product
quantity
```

Relationships:

- Many cart items belong to one cart
- Many cart items can reference one product

---

# Cart APIs

## Create Cart

```http
POST /cart
```

Creates authenticated user's cart.

---

## Get Current User Cart

```http
GET /cart
```

Returns full cart state.

---

## Add Product To Cart

```http
POST /cart/add/{productId}
```

Behavior:

- Creates cart item if absent
- Increments quantity if already exists

Example:

```http
POST /cart/add/1
```

---

## Remove Product From Cart

```http
POST /cart/remove/{productId}
```

Behavior:

- Decrements quantity
- Deletes cart item when quantity reaches zero

---

# Order System

The order system implements:

- Checkout flow
- Order persistence
- Order history
- Inventory reduction
- Snapshot purchase pricing

---

# Order Architecture

```text
User
 ↳ Orders
      ↳ OrderItems
           ↳ Product
```

---

## Order Entity

Fields:

```java
id
user
totalAmount
createdAt
orderItems
```

---

## OrderItem Entity

Fields:

```java
id
order
product
quantity
priceAtPurchase
```

Important:

```java
priceAtPurchase
```

stores historical purchase pricing.

This prevents future product price changes from affecting historical orders.

---

# Checkout Flow

Endpoint:

```http
POST /orders/checkout
```

Workflow:

```text
Authenticated User
        ↓
Fetch Cart
        ↓
Validate Cart
        ↓
Validate Inventory
        ↓
Create Order
        ↓
Create OrderItems
        ↓
Reduce Product Inventory
        ↓
Clear Cart
        ↓
Return Final Order
```

---

## Example Checkout Response

```json
{
  "id": 1,
  "totalAmount": 360.0,
  "orderItems": [
    {
      "product": {
        "name": "Protein Bar"
      },
      "quantity": 3,
      "priceAtPurchase": 120.0
    }
  ]
}
```

---

# Security Architecture

Current security implementation includes:

- Spring Security
- JWT authentication
- BCrypt password hashing
- Stateless session management
- Protected API routes

---

# Security Flow

```text
Login Request
      ↓
JWT Token Generated
      ↓
Client Stores Token
      ↓
Token Sent In Authorization Header
      ↓
JWT Filter Validates Token
      ↓
Spring Security Context Authenticated
      ↓
Protected APIs Accessible
```

---

# Global Exception Handling

Implemented using:

```java
@RestControllerAdvice
```

Features:

- Validation error handling
- Runtime exception handling
- Structured JSON error responses

Example Response:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid password"
}
```

---

# Docker Setup

## Dockerfile

Containerizes Spring Boot backend.

Base image:

```dockerfile
FROM eclipse-temurin:21-jdk
```

---

## Docker Compose

Services:

- backend
- postgres

Features:

- Persistent PostgreSQL storage
- Container networking
- Isolated deployment environment

---

# Database Design

## Tables

Implemented tables:

```text
users
products
carts
cart_items
orders
order_items
```

---

# Relational Model

```text
User
 ├── Cart
 │     └── CartItems
 │             └── Product
 │
 └── Orders
        └── OrderItems
                └── Product
```

---

# Project Structure

```text
src/main/java/com/sid/commerceapi
│
├── controller
│   ├── AuthController.java
│   ├── ProductController.java
│   ├── CartController.java
│   └── OrderController.java
│
├── entity
│   ├── User.java
│   ├── Product.java
│   ├── Cart.java
│   ├── CartItem.java
│   ├── Order.java
│   └── OrderItem.java
│
├── repository
│   ├── UserRepository.java
│   ├── ProductRepository.java
│   ├── CartRepository.java
│   ├── CartItemRepository.java
│   ├── OrderRepository.java
│   └── OrderItemRepository.java
│
├── dto
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   └── AuthResponse.java
│
├── security
│   ├── JwtUtil.java
│   └── JwtAuthenticationFilter.java
│
├── config
│   └── SecurityConfig.java
│
├── exception
│   └── GlobalExceptionHandler.java
│
└── CommerceapiApplication.java
```

---

# Running The Project

# Requirements

Install:

- Java 21
- Docker Desktop
- Maven

---

# Build Project

```powershell
.\mvnw clean install
```

---

# Build Docker Containers

```powershell
docker compose build --no-cache
```

---

# Start Containers

```powershell
docker compose up
```

---

# Stop Containers

```powershell
docker compose down
```

---

# Check Running Containers

```powershell
docker ps
```

---

# PostgreSQL Access

Open PostgreSQL shell:

```powershell
docker exec -it commerce-postgres psql -U sid -d commerce_db
```

---

# Example SQL Commands

View users:

```sql
SELECT * FROM users;
```

View products:

```sql
SELECT * FROM products;
```

View cart items:

```sql
SELECT * FROM cart_items;
```

View orders:

```sql
SELECT * FROM orders;
```

---

# Current Architectural Strengths

Implemented:

- JWT Authentication
- Stateless security
- BCrypt password hashing
- Relational persistence
- Transactional checkout flow
- Inventory management
- REST API architecture
- Global exception handling
- Dockerized deployment
- PostgreSQL persistence
- Bidirectional JPA relationships
- Layered backend architecture

---

# Current Limitations

Areas still needing improvement:

- DTO-based API responses
- Password leakage in entity serialization
- Role-based authorization
- Swagger/OpenAPI docs
- Unit testing
- Integration testing
- Pagination
- Product search
- Payment integration
- CI/CD pipeline
- Monitoring/logging

---

# Recommended Next Features

## Backend

- DTO refactor
- Role-based admin system
- Swagger/OpenAPI
- Payment gateway integration
- Pagination
- Search/filtering
- Redis caching
- Email notifications
- Order status tracking

---

## Frontend

Recommended stack:

- React
- Vite
- TailwindCSS
- Axios

Frontend pages:

- Login/Register
- Product Listing
- Product Details
- Cart Page
- Checkout Page
- Order History
- Admin Dashboard

---

# Deployment Roadmap

Potential deployment stack:

## Backend

- AWS EC2
- ECS Fargate
- Elastic Beanstalk

## Database

- AWS RDS PostgreSQL

## Container Registry

- AWS ECR

## CI/CD

- GitHub Actions

---

# Learning Outcomes

This project demonstrates:

- Spring Boot backend development
- JWT authentication systems
- Relational database modeling
- Transactional business logic
- Dockerized deployment
- REST API design
- Inventory management
- E-commerce backend architecture
- PostgreSQL integration
- Stateful commerce workflows

---

# Future Advanced Topics

Potential advanced backend topics:

- Microservices architecture
- Event-driven systems
- Kafka/SQS
- Redis caching
- Elasticsearch
- API Gateway
- Serverless deployment
- Distributed transactions
- CQRS/Event Sourcing
- Horizontal scaling

---

# Final Status

Commerce API has evolved from a simple CRUD application into a transactional e-commerce backend system with:

- Authentication
- Authorization
- Persistent storage
- Cart management
- Checkout workflows
- Inventory management
- Order persistence
- Dockerized infrastructure
- Relational commerce modeling

The project now represents a strong intermediate-to-advanced backend engineering portfolio project.

