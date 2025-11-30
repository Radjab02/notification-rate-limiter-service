## 📡 🔧 Technology Stack

* **Java 21**
* **Spring Boot 3.x**
* **Bucket4j (Redis-backed distributed rate limiting)**
* **Spring Data JPA**
* **H2 (Test) / PostgreSQL (Prod-ready)**
* **Redis (Lettuce client)**
* **JUnit 5 & Spring Boot Test**
* **Testcontainers**

## 📌 Features:

### ✅ Company-Level Configuration

* **Define per-client:**

* **Monthly request quota**

* **Windowed token bucket capacity**

* **Window duration (seconds)**

### ✅ Runtime Rate Limit Enforcement

* **Enforces global distributed limit**

* **Enforces per-client window rate limit**

* **Enforces per-client monthly quota**

* **Enforces limits consistently across multiple instances using Redis**


## 🧩 System Architecture

### ⭐ High-Level Components

- **RateLimitInterceptor**  
  Intercepts all incoming API requests and enforces:
    - global limit
    - monthly per-client limit
    - per-client sliding-window limit

- **RateLimitConfigService**  
  Loads rate-limit configurations from the database and constructs Redis-backed Bucket4j token buckets for enforcement.

- **Redis**  
  Stores all distributed token buckets (global + per-client), enabling horizontal scaling and consistent rate limiting across nodes.

- **H2 / PostgreSQL**  
  Persists client rate-limit configuration such as monthly limits and window settings.


### ✅ Admin Endpoints

| Method   | Path                       | Description                |
|----------|----------------------------|----------------------------|
| `GET`    | `/admin/limits`            | Get all client limits      |
| `GET`    | `/admin/limits/{clientId}` | Get limits for one client  |
| `POST`   | `/admin/limits`            | Create/update client limit |
| `DELETE` | `/admin/limits/{clientId}` | Delete client config       |


## 🔄 Request Flow Summary

1. **Angular frontend** → calls **`/api/notifications/send`**
2. **Interceptor** extracts `X-Client-ID`
3. **Global Redis bucket** (system-wide limit) is checked
4. **Monthly counter** (per-client, Redis) is checked
5. **Window bucket** (per-client, Redis sliding window) is checked
6. **If allowed:** request proceeds → controller → success response (`200 OK`)
7. **If denied:** system returns
    - `429 Too Many Requests` (rate limit exceeded), or
    - `402 Payment Required` (monthly quota exceeded)

## 🌐 Environment Configuration

Excerpt from `application.yml`:

```yaml
notification-rate-limiter-service:
  global-bucket:
    key: "rate_limit:global_system"
    capacity: 1000
    duration-in-minutes: 1
```


## 🗄️ Database Schema

### `client_limit_config` Table

```sql
CREATE TABLE client_limit_config (
    client_id VARCHAR(255) PRIMARY KEY,
    monthly_limit BIGINT NOT NULL,
    window_capacity INT NOT NULL,
    window_duration_seconds INT NOT NULL
); 
```

## 🧪 Tests

The system includes **full integration tests** using:

- **MockMvc**
- **Redis Testcontainers**
- **H2 in-memory database**
- **Spring Boot Test Framework**

### ✅ Test Coverage

The following use cases are fully tested end-to-end:

1. **Defining window-based limits**  
2. **Defining monthly limits**  
3. **Enforcing per-window rate limit**  
4. **Enforcing monthly quota**  
5. **Enforcing global Redis-backed system limit** 


## ▶️ Running the Service Locally

## A. Backend: 

### 1. Start Redis 
```text
   1.docker compose up -d
   2.docker ps -s #ensure redis is running
   ```
### 2. Run Spring Boot
```text
   ./gradlew bootRun or Run from IDE
   
   ```
### 3. Access H2 Console
```text
   http://localhost:8081/h2-console
   
   ```
### 4. JDBC URL (from config):
```text
   jdbc:h2:mem:corp_xyz_db
``` 


## B. Frontend: 
```text
   ng serve --open
``` 


