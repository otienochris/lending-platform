# Lending Platform

A cloud-native **lending platform** built using a **microservices architecture** and the **Saga pattern** to ensure data consistency, resilience, and scalability across distributed financial operations.

---

## 🧩 Overview

This platform supports the full loan lifecycle — from customer onboarding and product configuration to loan disbursement, repayment, and customer engagement. It is designed for **high reliability**, **fault tolerance**, and **independent service evolution**, which are critical in financial systems.

At the core of the platform is the **Saga pattern**, used to manage complex, multi-step business transactions that span multiple microservices.

---

## 🏗️ Architecture Summary

The system follows a **microservices-first design**, where each service:

* Owns its own database
* Is independently deployable
* Communicates asynchronously via events

  <img width="1267" height="781" alt="image" src="https://github.com/user-attachments/assets/aef91015-6f27-4579-bc05-715c3498cb47" />


### Key Services

* **Saga Orchestrator**
  Coordinates long-running business transactions such as loan disbursement and repayment using the Saga pattern.

* **Loan Disbursement Service**
  Handles loan approval execution, fund release, and disbursement status updates.

* **Loan Repayment Service**
  Manages repayment schedules, collections, penalties, and reconciliation.

* **Authentication Server**
  Provides secure authentication and authorization (OAuth2 / JWT-based).

* **Product Configuration Service**
  Manages loan products, interest rates, tenures, fees, and eligibility rules.

* **CRM Service**
  Maintains customer profiles, loan history, and engagement data.

* **Notification Service**
  Sends SMS, email, and push notifications for loan events (approval, disbursement, repayment reminders, failures).
* **ms-discovery**
  Service discovery and registration for internal communication.

  * NB: this service is also responsible for db migrations. all migration scripts and store and executed here via flyway;

---

## 🔄 Why the Saga Pattern?

### The Problem with Distributed Transactions

In a traditional monolithic system, a single database transaction ensures **atomicity** (all-or-nothing). However, in a microservices architecture:

* Each service has its **own database**
* A single business operation spans **multiple services**
* Using **2-phase commit (2PC)** is not scalable and introduces tight coupling

For a lending platform, operations like **loan disbursement** or **repayment processing** are:

* Long-running
* Business-critical
* Involving multiple independent services

This makes traditional ACID transactions impractical.

---

### What the Saga Pattern Solves

The **Saga pattern** breaks a large transaction into a sequence of **local transactions**, where:

* Each step updates its own service’s database
* Services communicate via events
* Failures are handled using **compensating actions**

This ensures **eventual consistency** without sacrificing system availability.

---

### Example: Loan Disbursement Saga

1. Orchestrator starts a `LoanDisbursementSaga`
2. Product Configuration validates loan terms
3. CRM confirms customer eligibility
4. Loan Disbursement releases funds
5. Notification service informs the customer

If any step fails:

* Previously completed steps are **compensated** (e.g., reverse disbursement, mark loan as failed)
* The system remains consistent

---

## 🎯 Why Saga Is Critical for a Lending Platform

* **Financial Safety** – Prevents partial loan disbursements or inconsistent balances
* **Resilience** – Failures in one service do not bring down the entire system
* **Scalability** – Services scale independently under high transaction volume
* **Auditability** – Each step of a business transaction is traceable
* **Regulatory Friendliness** – Clear transaction history and recovery logic

In financial systems where correctness matters more than immediacy, **eventual consistency with strong recovery guarantees** is the right trade-off.

---

## 🧠 Saga Implementation Style

This platform uses an **Orchestrated Saga** approach:

* A dedicated **Saga Orchestrator** controls the workflow
* Services remain simple and focused on domain logic
* Business flows are centralized and easier to reason about

This approach was chosen over choreography to:

* Avoid implicit coupling via event chains
* Improve observability and debugging
* Make business rules explicit

---

## 📦 Technology Stack (Example)

* Java / Spring Boot
* Reactive stack (Project Reactor)
* Apache Kafka (event-driven communication)
* OAuth2 / JWT (security)
* PostgreSQL (per-service databases)
* Docker & Docker Compose


---

## ▶️ How to Run the Application Locally

The platform is designed to be run locally using **Docker Compose**, which spins up all required infrastructure and microservices.

### Prerequisites

* Docker (v20+)
* Docker Compose (v2+)
* Java 17+ (only if running services outside Docker)
* Maven or Gradle

---

### 1️⃣ Clone the Repository

```bash
git clone https://github.com/otienochris/lending-platform.git
cd lending-platform
```

---

### 2️⃣ Build the Services
NB: mvn clean instal the commons service first.
Build all microservices using Maven (example):

```bash
mvn clean package -DskipTests
```

Each service produces a runnable JAR that is used by Docker.

---

### 3️⃣ Docker Compose Setup

Below is a **simplified Docker Compose** configuration for local development.

```yaml
version: '3.9'

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  postgres:
    image: postgres:15
    environment:
      POSTGRES_USER: lending
      POSTGRES_PASSWORD: lending
    ports:
      - "5432:5432"

  auth-server:
    build: ./auth-server
    depends_on:
      - postgres
    ports:
      - "9000:9000"

  saga-orchestrator:
    build: ./orchestrator
    depends_on:
      - kafka
      - postgres
    ports:
      - "8080:8080"

  loan-disbursement:
    build: ./loan-disbursement
    depends_on:
      - kafka
      - postgres

  loan-repayment:
    build: ./loan-repayment
    depends_on:
      - kafka
      - postgres

  product-configuration:
    build: ./product-configuration
    depends_on:
      - postgres

  crm:
    build: ./crm
    depends_on:
      - postgres

  notification:
    build: ./notification
    depends_on:
      - kafka
```

---

### 4️⃣ Start the Platform

Run the entire stack with:

```bash
docker compose up --build
```

All services will start and register with Kafka for event-driven communication.

---

### 5️⃣ Verify the System

* Saga Orchestrator: [http://localhost:8084](http://localhost:8080)
* Auth Server: [http://localhost:4011](http://localhost:9000)
* Kafka: localhost:9092
* PostgreSQL: localhost:5432

You can now trigger loan disbursement and repayment workflows via the orchestrator APIs.

---
1. Create a new user via the Auth Server API:
```bash
curl --location 'http://localhost:4011/api/v1/users' \
--header 'Content-Type: application/json' \
--data-raw '{
    "header": {
        "sourceSystem":"USSD",
        "operation": "USER_CREATION",
        "requestRefId": "df3db68a-dc26-4125-a634-63a8846af6cb",
        "correlationId":"bee7baec-1dd1-4e66-a5f2-841ec62734f7",
        "timestamp": "1770768271"
    },
    "body": {
        "username": "otienochris",
        "email":"ohtischris@gmail.com",
        "password":"password",
        "firstName":"Chris",
        "lastName":"Otieno",
        "msisdn":"254742887480"
    }
}'
```

2. User the username and password to login to the Auth Server UI:
```bash
curl --location 'http://localhost:4011/oauth/token?grant_type=password&username=otienochris&password=password' \
--header 'x-request-ref-id: b42c07f6-9eae-4d02-916c-845e6e1f1d43' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'username=otienochris' \
--data-urlencode 'password=password' \
--data-urlencode 'grant_type=password'
```

3. Create product:
  ``` bash
curl --location 'http://localhost:8080/api/v1/loan-products' \
--header 'X-Correlation-ID: 634712b8-187f-4dd5-a538-a4c7a109fb7c' \
--header 'X-Request-Ref-Id: 6789f454-42b4-4f66-ba1f-15d1049e8b64' \
--header 'X-Source-System: USSD' \
--header 'X-Operation: LOAN_PRODUCT_CONFIGURATION' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsInVzZSI6InNpZyIsImtpZCI6ImM4MDQzMmJlLTdhYTEtNGU4ZS05M2Q0LTJmYjQ0ZWY3NTY4NCJ9.eyJzdWIiOiJvdGllbm9jaHJpcyIsInJvbGVzIjpbIlJPTEVfQ1VTVE9NRVIiXSwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo0MDExIiwiaWQiOiJmMjkyZDUzYS1iNjcxLTQ4MDktYjUxNi02ZTJiNTIyZjQ2M2IiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJvdGllbm9jaHJpcyIsImV4cCI6MTc3MDc4OTkxOCwiaWF0IjoxNzcwNzUzOTE4fQ.ul3652-tuyhqraFjMa7rHbdN6ue-42IenBg-VyGruRe6MjZZ4_R27iVq-xbBOSt7TwqZqTX9LTvyrubm8LwbLQJbnB15EtBpoicQZo4jNgexBLYfbchz5tGPFzHkQVOQdstmK5nk8siElFfpS1lZKG_DvaTQ5oMicll67ZfZ_CG-6ewYvr_bTecw1u3aqLspEalGb6V_vzDf4Jpc0NBfRpJ8XMymySbw8MztSeMTtWq6pjafiTBQGuLN_jRqgKQIwcMeVQJDfQhUdLWPiIkJ-z2i_IvB5V74yE5Oqzaoh-xHn9pjEej2dJFBfiMbfbSX3P86w-wDHEjhJij5n4BjLg' \
--data '{
    "header": {
        "correlationId":"192c0ad4-b6fc-4003-9ebc-85e806465786",
        "sourceSystem":"USSD",
        "operation":"LOAN_PRODUCT_CONFIGURATION",
        "requestRefId":"0aa14679-59b7-4d03-8373-d84b3544842b",
        "timestamp":"1770768395"
    },
    "body" : {
        "productName":"Mkulima Loan",
        "minAmount":500,
        "maxAmount":1000000,
        "currency":"KES",
        "supportInstallments":true,
        "interestRate": 10,
        "interestRateType":"FLAT",
        "tenureOptions": [3,6,12],
        "tenureOptionsType": "MONTHS",
        "effectiveFrom":"2026-03-01T10:30:30",
        "effectiveTo":"2027-03-01T10:30:30"
    }
}'
`
```
4. Add fees:

  ```bash
  curl --location 'http://localhost:8080/api/v1/loan-products/3d6453dd-46e3-45c5-afd9-f6c87659ed57/fees' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsInVzZSI6InNpZyIsImtpZCI6ImM4MDQzMmJlLTdhYTEtNGU4ZS05M2Q0LTJmYjQ0ZWY3NTY4NCJ9.eyJzdWIiOiJvdGllbm9jaHJpcyIsInJvbGVzIjpbIlJPTEVfQ1VTVE9NRVIiXSwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo0MDExIiwiaWQiOiJmMjkyZDUzYS1iNjcxLTQ4MDktYjUxNi02ZTJiNTIyZjQ2M2IiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJvdGllbm9jaHJpcyIsImV4cCI6MTc3MDc4OTkxOCwiaWF0IjoxNzcwNzUzOTE4fQ.ul3652-tuyhqraFjMa7rHbdN6ue-42IenBg-VyGruRe6MjZZ4_R27iVq-xbBOSt7TwqZqTX9LTvyrubm8LwbLQJbnB15EtBpoicQZo4jNgexBLYfbchz5tGPFzHkQVOQdstmK5nk8siElFfpS1lZKG_DvaTQ5oMicll67ZfZ_CG-6ewYvr_bTecw1u3aqLspEalGb6V_vzDf4Jpc0NBfRpJ8XMymySbw8MztSeMTtWq6pjafiTBQGuLN_jRqgKQIwcMeVQJDfQhUdLWPiIkJ-z2i_IvB5V74yE5Oqzaoh-xHn9pjEej2dJFBfiMbfbSX3P86w-wDHEjhJij5n4BjLg' \
--data '{
    "header": {
        "correlationId":"24370cd3-9471-4167-8eb3-1b0e492fa8e9",
        "sourceSystem":"USSD",
        "operation":"LOAN_PRODUCT_CONFIGURATION",
        "requestRefId":"7565b8cb-578b-48cc-9918-8fdfbfe9e79d",
        "timestamp":"1770768427"
    },
    "body": [
        {
            "applicableAt":"DISBURSEMENT",
            "feeValueType" :"PERCENTAGE",
            "feeType":"SERVICE_FEE",
            "feeValue":10
        },
        {
            "applicableAt":"LIFETIME",
            "feeValueType" :"PERCENTAGE",
            "feeType":"DAILY_FEE",
            "feeValue":2
        },
        {
            "applicableAt":"PAST_DUE_DATE",
            "feeValueType" :"PERCENTAGE",
            "feeType":"LATE_FEE",
            "feeValue":1
        }
        
    ]
}'
  ```

5. get a loan

  ```bash
  
  curl --location 'http://localhost:8084/api/v1/loans' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsInVzZSI6InNpZyIsImtpZCI6ImM4MDQzMmJlLTdhYTEtNGU4ZS05M2Q0LTJmYjQ0ZWY3NTY4NCJ9.eyJzdWIiOiJvdGllbm9jaHJpcyIsInJvbGVzIjpbIlJPTEVfQ1VTVE9NRVIiXSwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo0MDExIiwiaWQiOiJmMjkyZDUzYS1iNjcxLTQ4MDktYjUxNi02ZTJiNTIyZjQ2M2IiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJvdGllbm9jaHJpcyIsImV4cCI6MTc3MDc4OTkxOCwiaWF0IjoxNzcwNzUzOTE4fQ.ul3652-tuyhqraFjMa7rHbdN6ue-42IenBg-VyGruRe6MjZZ4_R27iVq-xbBOSt7TwqZqTX9LTvyrubm8LwbLQJbnB15EtBpoicQZo4jNgexBLYfbchz5tGPFzHkQVOQdstmK5nk8siElFfpS1lZKG_DvaTQ5oMicll67ZfZ_CG-6ewYvr_bTecw1u3aqLspEalGb6V_vzDf4Jpc0NBfRpJ8XMymySbw8MztSeMTtWq6pjafiTBQGuLN_jRqgKQIwcMeVQJDfQhUdLWPiIkJ-z2i_IvB5V74yE5Oqzaoh-xHn9pjEej2dJFBfiMbfbSX3P86w-wDHEjhJij5n4BjLg' \
--data '{
    "header": {
        "correlationId":"872daea8-d82f-4f48-9733-fd450d7956b3",
        "sourceSystem":"USSD",
        "operation":"LOAN_APPLICATION",
        "requestRefId":"55458e4d-bd7f-4ef6-a5f3-20907b1e7394",
        "timestamp":"1770768477"
    },
    "body" : {
        "productId":"3d6453dd-46e3-45c5-afd9-f6c87659ed57",
        "loanAmount":600,
        "customerId":"f292d53a-b671-4809-b516-6e2b522f463b",
        "loanPurpose": "Development",
        "tenure":3,
        "walletId":"254742887480",
        "walletType":"Crypto",
        "installment": false
    }
}'
  ```

6. get loan repayment plan:

  ```bash
  curl --location 'http://localhost:8086/api/v1/loans?userId=f292d53a-b671-4809-b516-6e2b522f463b&loanStatus=OPEN' \
--header 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsInVzZSI6InNpZyIsImtpZCI6ImQwNDAxMWFjLWUzYzctNGU5Ni04ZDJjLTU0YWIxNjU5NDU1OCJ9.eyJzdWIiOiJvdGllbm9jaHJpcyIsInJvbGVzIjpbIlJPTEVfQ1VTVE9NRVIiXSwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo0MDExIiwiaWQiOiJmMjkyZDUzYS1iNjcxLTQ4MDktYjUxNi02ZTJiNTIyZjQ2M2IiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJvdGllbm9jaHJpcyIsImV4cCI6MTc3MDgwNDYwNiwiaWF0IjoxNzcwNzY4NjA2fQ.CTJgcfavkfAS-Q8ftYRQvv20t-guol3LMCefX4Rb68iebI2Wo9W0Z1wPSYxqqh-yn3y_u6sa013UDFRAhkwy1lc8bQUW6SLefzjM0KNxU3ZDVRpUFR9qF7EFJSDiiHSCtDnnLVgesNkObxg22EHFXB2ZMz2vl9bxD-LMxoIh29tSvmDsvCyMoM-FdlnRUL5Ha8rdZVKQr2AKNzkeKKmxKwn6yKpgVPSgtMj7pGvFVzCnlWgCXZ9LO8e2avP4Zx7QQo_KeWw7T8eq4ew4MsqAg2scjA_xVNe0y13Om99MzADfTP7P95aqEsST7Qh7n7cqLS7hC4SvLrotoO7rBjQJAw'
  ```

7. Repay

  ```bash 
  curl --location 'http://localhost:8084/api/v1/loans/repayment' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsInVzZSI6InNpZyIsImtpZCI6ImM4MDQzMmJlLTdhYTEtNGU4ZS05M2Q0LTJmYjQ0ZWY3NTY4NCJ9.eyJzdWIiOiJvdGllbm9jaHJpcyIsInJvbGVzIjpbIlJPTEVfQ1VTVE9NRVIiXSwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo0MDExIiwiaWQiOiJmMjkyZDUzYS1iNjcxLTQ4MDktYjUxNi02ZTJiNTIyZjQ2M2IiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJvdGllbm9jaHJpcyIsImV4cCI6MTc3MDc4OTkxOCwiaWF0IjoxNzcwNzUzOTE4fQ.ul3652-tuyhqraFjMa7rHbdN6ue-42IenBg-VyGruRe6MjZZ4_R27iVq-xbBOSt7TwqZqTX9LTvyrubm8LwbLQJbnB15EtBpoicQZo4jNgexBLYfbchz5tGPFzHkQVOQdstmK5nk8siElFfpS1lZKG_DvaTQ5oMicll67ZfZ_CG-6ewYvr_bTecw1u3aqLspEalGb6V_vzDf4Jpc0NBfRpJ8XMymySbw8MztSeMTtWq6pjafiTBQGuLN_jRqgKQIwcMeVQJDfQhUdLWPiIkJ-z2i_IvB5V74yE5Oqzaoh-xHn9pjEej2dJFBfiMbfbSX3P86w-wDHEjhJij5n4BjLg' \
--data '{
    "header": {
        "correlationId":"f5483a27-23cb-4fe7-baae-140ff54c53f8",
        "sourceSystem":"USSD",
        "operation":"LOAN_REPAYMENT",
        "requestRefId":"6d53bce5-c435-4d15-88e9-8d1cc198a233",
        "timestamp":"1770768703"
    },
    "body" : {
        "loanScheduleId":"42cba215-6dc4-44df-8133-d98486ed6285",
        "amount":1000,
        "customerId":"f292d53a-b671-4809-b516-6e2b522f463b",
        "walletId":"254742887480",
        "walletType":"MobileMoney"
    }
}'
  ```
---

## 🚀 Key Design Principles

* Domain-driven service boundaries
* Event-driven communication
* Idempotent message handling
* Compensating transactions over rollbacks
* Observability-first (logs, metrics, tracing)

---

## ⚖️ Challenges & Trade-offs

Building a Saga-based lending platform comes with clear benefits, but also important trade-offs that were consciously made.

### 1. Eventual Consistency vs Immediate Consistency

**Challenge:**

* Data across services is not immediately consistent.
* For example, a loan may appear as *pending* in one service while another has already completed its step.

**Trade-off & Rationale:**

* In distributed financial systems, *availability and fault tolerance* are prioritized over immediate consistency.
* Eventual consistency is acceptable as long as compensations and audit trails are robust.

---

### 2. Increased System Complexity

**Challenge:**

* Introducing sagas adds complexity in terms of orchestration logic, state management, and error handling.
* Debugging distributed workflows is harder than debugging a monolith.

**Trade-off & Rationale:**

* The complexity is intentional and localized within the **Saga Orchestrator**, keeping domain services simple.
* This avoids hidden coupling and unpredictable event chains.

---

### 3. Compensating Transactions Are Not True Rollbacks

**Challenge:**

* Compensations are business-driven actions (e.g., reversing a disbursement), not database rollbacks.
* Some actions (notifications, external integrations) are irreversible.

**Trade-off & Rationale:**

* Financial systems require *business-level recovery*, not technical rollbacks.
* Every step is designed to be idempotent and auditable, ensuring safe retries and traceability.

---

### 4. Orchestration vs Choreography

**Challenge:**

* Orchestration introduces a central coordinator, which could become a bottleneck if poorly designed.

**Trade-off & Rationale:**

* Orchestration was chosen to make business workflows explicit, debuggable, and observable.
* This is especially important in lending systems where business rules frequently change and must be clearly enforced.

---

### 5. Operational Overhead

**Challenge:**

* Requires strong observability (logs, metrics, distributed tracing).
* More infrastructure components (message broker, monitoring, retries).

**Trade-off & Rationale:**

* The added operational cost is justified by higher system resilience, regulatory friendliness, and scalability.

---

## 📌 Conclusion

This lending platform demonstrates how the **Saga pattern** enables safe, scalable, and resilient financial workflows in a distributed microservices environment. By embracing eventual consistency, explicit compensation logic, and orchestration-driven control, the system achieves both **business correctness** and **engineering scalability** — qualities that are essential for real-world digital lending systems.
