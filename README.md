# 🎟️ ReserveX

A distributed ticket reservation backend built using *Spring Boot, **Redis, **PostgreSQL, **Docker, and **JWT Authentication*, designed to handle concurrent seat booking while maintaining data consistency and secure user access.

---

## 🚀 Overview

ReserveX is a scalable movie ticket reservation backend that simulates the core architecture of modern booking platforms.

The application provides secure authentication, movie and show management, seat reservation, booking lifecycle management, distributed seat locking using Redis, pagination, search APIs, and containerized deployment using Docker.

---

# ✨ Features

### 👤 Authentication
- User Registration
- JWT Authentication
- Role-based authorization
- Secure password encryption using BCrypt

### 🎬 Movie Management
- Create, Update, Delete Movies
- Paginated movie listing
- Search by title
- Search by language
- Search by genre

### 🎭 Show Management
- Create and manage movie shows
- Automatic seat generation (120 seats per show)
- Search shows by movie
- Search shows by date
- Pagination support

### 🎟️ Booking System
- Secure ticket booking
- Booking reference generation
- Booking expiry support
- Booking cancellation
- Seat availability tracking

### 🔒 Distributed Seat Locking
- Redis-based distributed locking
- Prevents double booking
- Handles concurrent booking requests safely

### 📊 Monitoring
- Swagger/OpenAPI documentation
- Dockerized deployment
- PostgreSQL persistence

---

# 🏗️ Architecture


                 Client

                   │

            REST API (Spring Boot)

                   │

     ┌─────────────┼──────────────┐

 Authentication   Booking     Movie/Show

                   │

         Redis Distributed Lock

                   │

            PostgreSQL Database


---

# 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 21 |
| Framework | Spring Boot |
| Security | Spring Security, JWT |
| Database | PostgreSQL |
| Cache | Redis |
| ORM | Spring Data JPA |
| Documentation | Swagger / OpenAPI |
| Containerization | Docker |
| Build Tool | Maven |
| Testing | JUnit 5, Mockito |

---

# 📂 Project Structure


src
 ├── controller
 ├── service
 ├── repository
 ├── entity
 ├── dto
 ├── exception
 ├── security
 ├── redis
 └── config


---

# 🔐 Authentication Flow

1. User registers.
2. Password is encrypted using BCrypt.
3. User logs in.
4. JWT token is generated.
5. Token is used to access secured endpoints.

---

# 🎟️ Booking Flow

1. User selects a show.
2. Requested seats are locked using Redis.
3. Seat availability is verified.
4. Booking is created.
5. Seats are marked as LOCKED.
6. Booking reference is generated.
7. Booking expires automatically if payment is not completed within the configured duration.

---

# 📖 REST APIs

### Authentication
- Register
- Login

### Movies
- Create Movie
- Get Movie
- Update Movie
- Delete Movie
- Search Movies

### Shows
- Create Show
- Get Shows
- Search Shows
- Update Show
- Delete Show

### Booking
- Create Booking
- View My Bookings
- Cancel Booking

---

# 🧪 Testing

Implemented *19+ unit tests* using *JUnit 5* and *Mockito* covering:

- CRUD operations
- Pagination
- Search functionality
- Exception handling
- Service layer validation

---

# 🐳 Docker

The application can be started using Docker Compose.

bash
docker-compose up --build


---

# 📈 Future Enhancements

- Payment Gateway Integration
- Email Notifications
- Seat Selection UI
- Rate Limiting
- Kubernetes Deployment
- CI/CD Pipeline
- Event-driven architecture using Kafka

---
GitHub:
https://github.com/Nehajain1207
