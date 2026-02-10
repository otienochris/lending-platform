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
git clone https://github.com/your-username/lending-platform.git
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

* Saga Orchestrator: [http://localhost:8080](http://localhost:8080)
* Auth Server: [http://localhost:9000](http://localhost:9000)
* Kafka: localhost:9092
* PostgreSQL: localhost:5432

You can now trigger loan disbursement and repayment workflows via the orchestrator APIs.

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
