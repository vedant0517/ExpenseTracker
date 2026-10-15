# Personal Expense Tracker — Spring Boot REST API & JWT Security

A **production-grade Personal Expense Tracker backend** engineered in **Java 17** and **Spring Boot 3.3.4**, featuring stateless **Spring Security with JWT authentication**, multi-tenant user isolation with **IDOR vulnerability defenses**, automated database versioning with **Flyway**, interactive documentation via **OpenAPI 3 / Swagger UI**, relational persistence with **Spring Data JPA** (PostgreSQL/MySQL/H2), and 55 automated unit, slice, and integration tests with **JUnit 5 and Mockito**.

> [!NOTE]
> **Security & Tenancy Notice:** The application implements strict multi-tenant isolation. Every transaction is permanently associated with its authenticated owner. All read, update, and delete operations enforce user ownership directly in the persistence layer. Attempts to access or mutate another user's transactions yield a `404 Not Found` response to prevent IDOR enumeration.

---

## Table of Contents
- [Problem & Motivation](#problem--motivation)
- [Key Features](#key-features)
- [Architecture & Design](#architecture--design)
- [Database Schema & ER Diagram](#database-schema--er-diagram)
- [Technology Stack](#technology-stack)
- [Authentication & Security Flow](#authentication--security-flow)
- [OpenAPI & Swagger UI Documentation](#openapi--swagger-ui-documentation)
- [Database Migrations with Flyway](#database-migrations-with-flyway)
- [API Reference](#api-reference)
- [Prerequisites & Local Setup](#prerequisites--local-setup)
- [Automated Testing & CI](#automated-testing--ci)
- [Production Deployment Guide](#production-deployment-guide)
- [Known Limitations & Future Roadmap](#known-limitations--future-roadmap)

---

## Problem & Motivation
Managing personal finances requires precision, zero rounding error, strict tenant data segregation, and reliable data modeling. Many expense trackers suffer from floating-point roundoff issues (using `double`), lack input boundaries, leave APIs unauthenticated, or permit Insecure Direct Object References (IDOR) where users can view or tamper with each other's records.

This project addresses these challenges by:
1. Enforcing **stateless Spring Security with JWT** so every API interaction is authenticated and authorized.
2. Implementing **strict tenant ownership** at the database query level (`WHERE user_id = :userId`) to prevent IDOR vulnerabilities.
3. Modeling monetary amounts with arbitrary-precision **`BigDecimal`** (enforcing 2 decimal places with `HALF_UP` rounding).
4. Automating database schema evolution using **Flyway versioned migrations** while setting Hibernate to strict `validate` mode.
5. Providing full interactive API explorer and documentation using **OpenAPI 3 and Swagger UI** with Bearer JWT support.
6. Implementing a strict **DTO boundary pattern** with Jakarta Bean Validation to eliminate over-posting risks and malformed inputs.
7. Supporting zero-configuration local development (in-memory H2) and persistent relational databases (PostgreSQL / MySQL).

---

## Key Features
- **Stateless JWT Authentication & BCrypt:**
  - Secure user registration (`/api/auth/register`) and login (`/api/auth/login`).
  - Passwords hashed with adaptive BCrypt hashing.
  - Cryptographically signed HMAC-SHA256 JWT access tokens with configurable expiration.
- **Multi-Tenant Isolation & IDOR Defenses:**
  - Every transaction is associated with its authenticated owner (`user_id`).
  - Service and repository methods guarantee users can only view, filter, aggregate, update, or delete their own transactions.
  - Requesting an ID belonging to another user returns `404 Not Found`, preventing IDOR existence disclosure.
- **Versioned Database Migrations (Flyway):**
  - Database schema managed via reproducible SQL migration scripts (`V1__create_users_and_transactions_tables.sql`).
  - Hibernate operates in `validate` mode in production to ensure entity-schema synchronization without unexpected DDL mutations.
- **Interactive OpenAPI & Swagger UI:**
  - Built-in Swagger UI with Bearer JWT authorization at `/swagger-ui/index.html`.
  - Machine-readable OpenAPI spec at `/v3/api-docs`.
- **Transaction CRUD & Analytics:**
  - Create, read, update, and delete income and expense records.
  - Dynamic multi-criteria filtering by `type`, `category` substring, and date ranges.
  - Pageable pagination and sorting.
  - Financial analytics: net balance, income/expense totals, category spend percentage breakdown, highest expense detection.
- **Crash-Proof Global Exception Handling:**
  - RFC 7807-style JSON error responses (`ErrorResponseDto`) with structured field validation messages and secure 401/404/400 status codes.
- **Cloud-Ready Health Checks:** Integrated Spring Boot Actuator (`/actuator/health`).

---

## Architecture & Design

The application follows a **Hexagonal / Clean Layered Architecture** with Spring Security intercepting all requests:

```mermaid
graph TD
    Client["Client (Browser / Postman / Swagger UI)"] -->|HTTP + Bearer JWT| FilterChain["Spring Security Filter Chain<br/>(JwtAuthenticationFilter)"]
    FilterChain -->|Validate Token & Set SecurityContext| Controller["Presentation Layer: REST Controllers<br/>(@RestController, @Valid)"]
    Controller -->|DTOs + Principal| Service["Business Logic Layer: Services<br/>(@Service, @Transactional)"]
    Service -->|User-Scoped Queries| Repository["Data Access Layer: Spring Data JPA<br/>(@Repository, JPQL with userId)"]
    Repository -->|JDBC / SQL| Database["Relational Database<br/>(PostgreSQL / MySQL / H2 managed by Flyway)"]
    Controller -.->|Exceptions Caught by| Advice["Global Exception Handler<br/>(@RestControllerAdvice)"]
    Advice -.->|JSON Error Response| Client
```

### Layer Responsibilities
- **Security (`security`):** JWT token issuance/validation, token extraction filter (`JwtAuthenticationFilter`), stateless security chain (`SecurityConfig`), and user details loading (`CustomUserDetailsService`).
- **Presentation (`controller`):** Handles HTTP status codes, routing, pagination arguments, and request validation. Zero direct SQL or business logic.
- **Data Transfer Objects (`dto`):** Immutable Java 17 records and validated request objects decoupling the external API contract from internal database schemas.
- **Business Logic (`service`):** Transactional business workflows, math calculations, authentication verification, and Entity <-> DTO conversions.
- **Data Access (`repository`):** Spring Data JPA dynamic query methods and JPQL projections scoped strictly to the authenticated `user_id`.
- **Domain (`entity`):** Relational entity mapping (`User`, `Transaction`) with column constraints, unique indexes, and foreign keys.

---

## Database Schema & ER Diagram

The relational schema is managed by Flyway migrations (`db/migration/V1__create_users_and_transactions_tables.sql`):

```mermaid
erDiagram
    USERS ||--o{ TRANSACTIONS : "owns (1:N)"
    USERS {
        bigint id PK "Generated Identity"
        varchar username UK "NOT NULL, max 50"
        varchar email UK "NOT NULL, max 100"
        varchar password "NOT NULL, BCrypt Hash, max 255"
        timestamp created_at "DEFAULT CURRENT_TIMESTAMP"
    }
    TRANSACTIONS {
        bigint id PK "Generated Identity"
        bigint user_id FK "NOT NULL -> users(id) ON DELETE CASCADE"
        varchar title "NOT NULL, max 255"
        decimal amount "NOT NULL, precision 12, scale 2"
        varchar category "NOT NULL, max 100"
        varchar type "NOT NULL, ENUM ('INCOME', 'EXPENSE')"
        date date "NOT NULL, YYYY-MM-DD"
        text description "Optional details"
        timestamp created_at "DEFAULT CURRENT_TIMESTAMP"
    }
```

### Constraints & Indexes
- `users`: Unique indexes on `username` and `email`.
- `transactions`: Foreign key `fk_transactions_user` references `users(id)` with `ON DELETE CASCADE`.
- Indexes: `idx_transactions_user_id`, `idx_transactions_user_date` (composite for user-scoped date queries), and `idx_transactions_user_category`.
- Check constraints: `chk_transaction_type` enforces `INCOME` or `EXPENSE`; `chk_transaction_amount` enforces amount > 0.

---

## Technology Stack
- **Language:** Java 17 LTS (Java 25 LTS compatible)
- **Framework:** Spring Boot 3.3.4
- **Security:** Spring Security 6, JJWT 0.12.6, BCrypt Password Hashing
- **API Documentation:** Springdoc OpenAPI 2.6.0 (OpenAPI 3 / Swagger UI)
- **Database Migrations:** Flyway Core & Flyway MySQL
- **Persistence & ORM:** Spring Data JPA, Hibernate 6
- **Database:** PostgreSQL (production), MySQL (supported), H2 (in-memory dev/test)
- **Validation:** Jakarta Bean Validation (Hibernate Validator)
- **Monitoring:** Spring Boot Actuator
- **Testing:** JUnit 5, Mockito, Spring Security Test, Spring Boot MockMvc, AssertJ
- **Build & Dependency Tool:** Apache Maven

---

## Authentication & Security Flow

### 1. User Registration (`POST /api/auth/register`)
- Accepts `username`, `email`, and `password` (min 8 characters).
- Hashes password with BCrypt (strength 10).
- Issues a 24-hour signed JWT access token upon successful registration.

### 2. User Login (`POST /api/auth/login`)
- Accepts `usernameOrEmail` and `password`.
- Validates credentials against BCrypt hash in database.
- Issues a signed JWT access token.

### 3. Accessing Protected Endpoints
- Include the token in the HTTP `Authorization` header:
  ```http
  Authorization: Bearer <your_jwt_token_here>
  ```
- Public Endpoints (no token required):
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `GET /swagger-ui/**`, `GET /swagger-ui.html`, `GET /v3/api-docs/**`
  - `GET /actuator/health`
- Protected Endpoints:
  - All `/api/transactions/**` endpoints.
  - All `/api/reports/**` endpoints.

---

## OpenAPI & Swagger UI Documentation

The application automatically exposes interactive API documentation:
- **Swagger UI Web Console:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) (or [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html))
- **OpenAPI 3 JSON Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### How to Test Protected Endpoints in Swagger UI:
1. Navigate to `/swagger-ui/index.html` in your browser.
2. Open the `Authentication` section and execute `POST /api/auth/register` or `POST /api/auth/login`.
3. Copy the `token` from the JSON response.
4. Click the green **Authorize** button at the top right of the Swagger UI page.
5. Enter the token in the **Value** field (`Bearer <token>` or `<token>`) and click **Authorize**.
6. All subsequent requests in Swagger UI will automatically include the `Authorization` header.

---

## Database Migrations with Flyway

Flyway manages schema versioning automatically on application startup.
- **Migration Location:** `src/main/resources/db/migration/`
- **Initial Migration:** `V1__create_users_and_transactions_tables.sql`
- **Hibernate Configuration:** In production and local setups, `spring.jpa.hibernate.ddl-auto` is set to `validate`, ensuring Flyway is the single source of truth for database modifications.

### Running Migrations Manually
When running via Maven or Spring Boot, Flyway detects pending migrations and applies them:
```bash
mvn flyway:info      # View migration history and status
mvn flyway:migrate   # Run pending migrations
```

---

## API Reference

### Base URL: `http://localhost:8080/api`

| Method | Endpoint | Description | Auth Required | Status Code |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/auth/register` | Register new user account | No | `201 Created` |
| `POST` | `/auth/login` | Authenticate and obtain JWT | No | `200 OK` |
| `POST` | `/transactions` | Create a transaction for current user | **Yes (Bearer)** | `201 Created` |
| `GET` | `/transactions` | List user transactions (paged & filtered) | **Yes (Bearer)** | `200 OK` |
| `GET` | `/transactions/{id}` | Retrieve transaction by ID (owner only) | **Yes (Bearer)** | `200 OK` |
| `PUT` | `/transactions/{id}` | Update transaction by ID (owner only) | **Yes (Bearer)** | `200 OK` |
| `DELETE` | `/transactions/{id}` | Delete transaction by ID (owner only) | **Yes (Bearer)** | `204 No Content` |
| `GET` | `/reports/summary` | User's income, expense, balance summary | **Yes (Bearer)** | `200 OK` |
| `GET` | `/reports/categories` | User's category-wise expense breakdown | **Yes (Bearer)** | `200 OK` |
| `GET` | `/actuator/health` | Service health status | No | `200 OK` |

---

### Sample Requests & Responses

#### 1. User Registration (`POST /api/auth/register`)
**Request Payload:**
```json
{
  "username": "alex_dev",
  "email": "alex@example.com",
  "password": "Password123!"
}
```
**Response (`201 Created`):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsIn...",
  "type": "Bearer",
  "username": "alex_dev",
  "email": "alex@example.com"
}
```

#### 2. Create Transaction (`POST /api/transactions`)
**Headers:**
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsIn...
Content-Type: application/json
```
**Request Payload:**
```json
{
  "title": "Software Engineering Salary",
  "amount": 6500.00,
  "category": "Salary",
  "type": "INCOME",
  "date": "2026-10-01",
  "description": "Monthly direct deposit"
}
```
**Response (`201 Created`):**
```json
{
  "id": 1,
  "title": "Software Engineering Salary",
  "amount": 6500.00,
  "category": "Salary",
  "type": "INCOME",
  "date": "2026-10-01",
  "description": "Monthly direct deposit"
}
```

#### 3. Financial Summary (`GET /api/reports/summary`)
**Response (`200 OK`):**
```json
{
  "totalIncome": 6500.00,
  "totalExpenses": 185.50,
  "netBalance": 6314.50,
  "transactionCount": 2,
  "highestExpense": 185.50,
  "averageExpense": 185.50
}
```

---

## Prerequisites & Local Setup

### Prerequisites
- **Java:** JDK 17 or higher (`java -version`)
- **Maven:** Version 3.8+ (`mvn -version`)
- **Database:** PostgreSQL or MySQL (optional; defaults to in-memory H2 if no DB environment variables are set)

### Environment Variables
| Variable | Description | Default (Local / Test) |
| :--- | :--- | :--- |
| `JWT_SECRET` | 256-bit+ base64/hex secret key for signing tokens | Pre-configured secure default in `application.properties` |
| `JWT_EXPIRATION_MS` | Token lifespan in milliseconds | `86400000` (24 hours) |
| `DB_URL` | JDBC database connection string | `jdbc:h2:mem:expensedb;MODE=PostgreSQL...` |
| `DB_USER` | Relational database username | `sa` |
| `DB_PASSWORD` | Relational database password | *(empty)* |
| `DB_DRIVER` | JDBC driver class name | `org.h2.Driver` |

### Option 1: Quick Start with In-Memory H2 (Zero Configuration)
The default configuration runs an in-memory H2 database with PostgreSQL compatibility mode:
```bash
mvn spring-boot:run
```
The application starts at `http://localhost:8080`.
Flyway initializes tables automatically on startup.

### Option 2: Running with Persistent PostgreSQL or MySQL
1. Create database:
   ```sql
   CREATE DATABASE expense_tracker_db;
   ```
2. Configure environment variables:
   ```bash
   # Windows PowerShell
   $env:DB_URL="jdbc:postgresql://localhost:5432/expense_tracker_db"
   $env:DB_USER="postgres"
   $env:DB_PASSWORD="your_password"
   $env:DB_DRIVER="org.postgresql.Driver"
   $env:JWT_SECRET="ProductionSecretKeyAtLeast32BytesLongForHMACSHA256SignatureSecurity"

   # Linux/macOS
   export DB_URL="jdbc:postgresql://localhost:5432/expense_tracker_db"
   export DB_USER="postgres"
   export DB_PASSWORD="your_password"
   export DB_DRIVER="org.postgresql.Driver"
   export JWT_SECRET="ProductionSecretKeyAtLeast32BytesLongForHMACSHA256SignatureSecurity"
   ```
3. Start the application:
   ```bash
   mvn spring-boot:run
   ```

---

## Automated Testing & CI

The project maintains an automated test pyramid across domain, service, repository, and controller layers:

```bash
# Run full verification suite (clean, compile, test, package)
mvn clean verify
```

### Test Suite Summary (55 Automated Tests):
- **`ExpenseManagerTest` (14 tests):** Core Java unit and integration tests covering domain operations, `BigDecimal` calculations, Lambda filtering and sorting, CSV file parsing resilience, and Plain JDBC CRUD with explicit transaction rollback.
- **`TransactionJpaRepositoryTest` (8 tests):** Data layer tests verifying tenant isolation, cross-tenant IDOR prevention, user-scoped aggregations, pagination, and date filtering.
- **`TransactionServiceTest` (14 tests):** Unit tests using JUnit 5 and Mockito verifying multi-tenant ownership enforcement, cross-tenant 404 IDOR rejection, authenticated user extraction, DTO mapping, and exact financial math.
- **`TransactionControllerTest` (10 tests):** Web layer MockMvc integration tests testing status codes (`201`, `200`, `204`, `400`, `404`), unauthenticated 401 rejection, cross-tenant 404 response, and validation envelopes.
- **`ReportControllerTest` (3 tests):** Controller integration tests verifying authenticated user financial summaries and category breakdown projections.
- **`AuthControllerTest` (6 tests):** Integration tests verifying user registration, duplicate username rejection (`400`), login authentication (`200`), invalid credentials (`401`), JWT Bearer protected route access (`200`), and malformed token rejection (`401`).

**Verified Test Execution Results:**
```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 55, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Continuous Integration (CI)
Automated testing is integrated via GitHub Actions in [.github/workflows/maven.yml](.github/workflows/maven.yml), running `mvn -B clean verify` with Eclipse Temurin JDK 17 and Maven dependency caching on every push and pull request to the `main` branch.

---

## Production Deployment Guide

### Deployment Architecture
```
  [User Traffic] ──HTTPS──> [Reverse Proxy / Cloud Ingress]
                                       │
                                       ▼
                         [Spring Boot Container] (Port 8080)
                                       │
                                       ▼
                     [Managed PostgreSQL (AWS RDS / Supabase)]
```

### Deployment Checklist:
1. **Hosting:** Deploy the Spring Boot JAR on platforms such as **Render**, **Railway**, **AWS Elastic Beanstalk**, or **Google Cloud Run** with the packaged `mvn clean package` artifact.
2. **Environment Variables:** Provide production credentials exclusively via environment variables:
   - `JWT_SECRET`: Secure, high-entropy secret (minimum 256 bits).
   - `DB_URL`: `jdbc:postgresql://<host>:<port>/<dbname>?sslmode=require`
   - `DB_USER`: database user
   - `DB_PASSWORD`: database password
   - `DB_DRIVER`: `org.postgresql.Driver`
3. **Health Check Probes:** Point load balancer probes to `/actuator/health`. Unauthenticated detail exposure is disabled (`management.endpoint.health.show-details=never`).
4. **HTTPS & Security:** Enforce SSL at the cloud ingress/load balancer level. Local secrets and properties are excluded from version control via [.gitignore](.gitignore).

---

## Known Limitations & Future Roadmap

### Implemented in Current Release:
- Stateless Spring Security 6 with JJWT access token authentication.
- BCrypt password encryption for user credentials.
- Strict tenant data isolation and IDOR defense (owner-scoped JPA queries).
- Versioned Flyway migrations (`V1__create_users_and_transactions_tables.sql`) with Hibernate `validate` mode.
- Interactive OpenAPI 3 / Swagger UI with Bearer JWT authorization.
- Full CRUD operations with auto-generated identity keys.
- Accurate monetary calculations using `BigDecimal` (scale 2, `HALF_UP`).
- Dedicated JPQL count queries and dynamic pagination/filtering.
- RFC 7807-style JSON error payloads and input validation.
- Minimal unauthenticated health check endpoint.
- 55 automated unit, slice, and integration tests with CI pipeline.

### Future Roadmap:
- [ ] **Refresh Token Rotation:** Add refresh token rotation to allow persistent sessions without extending access token validity.
- [ ] **Rate Limiting:** Implement IP/user-based request rate limiting (Bucket4j or Redis) on login and registration endpoints.
- [ ] **Email Verification & Password Reset:** Implement transactional email verification for new account registrations.
- [ ] **Data Export Endpoints:** Expose CSV/Excel export endpoints for periodic ledger backups.
- [ ] **Frontend Web Application:** Connect a responsive single-page application (React / Vite).
