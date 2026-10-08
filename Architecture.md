# Architecture Overview

## 1. Purpose

This document defines the high-level architecture of the Retail Inventory System.

The system is designed as a microservice-based application using CQRS for the Inventory domain. It supports inventory management, inventory querying, and back-in-stock notifications for a single store.

The architecture is designed to demonstrate reliable asynchronous processing, resiliency, and separation of command and query responsibilities without introducing unnecessary infrastructure.

---

## 2. Architectural Style

The system follows a microservice architecture consisting of independently deployable services.

The Inventory Service uses the CQRS pattern to separate inventory write operations from inventory read operations.

The system uses:

- REST for synchronous operations that require an immediate response.
- Kafka for asynchronous event propagation.
- PostgreSQL for transactional and authoritative data.
- Couchbase for the Inventory query model.
- Transactional Outbox for reliable publication of inventory events.

---

## 3. System Components

### 3.1 User Service

Responsible for:

- User registration.
- User authentication.
- User authorization information.

The User Service manages Customer and Store Manager accounts.

### 3.2 Inventory Service

Responsible for:

- Managing product inventory.
- Increasing product inventory.
- Reducing product inventory.
- Validating inventory availability.
- Handling inventory reduction idempotency.
- Providing inventory queries.

The Inventory Service uses CQRS:

- PostgreSQL is the source of truth for inventory changes.
- Couchbase provides the read-optimized inventory query model.

The Inventory Service also publishes inventory changes for asynchronous processing.

### 3.3 Order Service

The Order Service represents the external system that requests inventory reduction when an order requires inventory to be deducted.

Order creation, payment, delivery, and order lifecycle management are outside the scope of this system.

The Order Service communicates synchronously with the Inventory Service.

### 3.4 Notification Service

Responsible for:

- Creating back-in-stock notification requests.
- Managing active notification requests.
- Processing product availability events.
- Creating user notifications.
- Managing notification read status.

The Notification Service uses PostgreSQL for its persistent data.

---

## 4. High-Level Architecture

The following diagram shows the primary service and infrastructure relationships.

    Customer
        |
        | REST
        v
    User Service
        |
        | Authentication information
        |
        +-----------------------------------+
                                            |
    Store Manager                           |
        |                                   |
        | REST                              |
        v                                   |
    Inventory Service <---------------------+
        |
        +-------------------+
        |                   |
        v                   v
    PostgreSQL          Couchbase
    Source of Truth     Query Model
        |
        | Transactional Outbox
        v
      Kafka
        |
        +-----------------------------+
        |                             |
        v                             v
    Query Model Consumer        Notification Consumer
        |                             |
        v                             v
    Couchbase                  Notification PostgreSQL


    Order Service
        |
        | REST
        v
    Inventory Service

The User Service provides authentication and authorization capabilities. Other services validate the authentication token when handling protected requests rather than routing business requests through the User Service.

---

## 5. Service Communication

The system uses synchronous REST communication where the caller requires an immediate response and asynchronous Kafka communication where processing can happen independently.

### 5.1 Synchronous Communication

REST is used for operations that require an immediate response.

Examples include:

- User registration and authentication.
- Store Manager inventory updates.
- Order Service inventory reduction requests.
- Customer inventory queries.
- Customer notification requests.
- Customer notification retrieval and read operations.

For example:

    Order Service
          |
          | Reduce Inventory
          v
    Inventory Service
          |
          v
    PostgreSQL

The Inventory Service completes the transactional inventory operation before returning the result to the Order Service.

### 5.2 Asynchronous Communication

Kafka is used to propagate inventory changes after the inventory transaction has been successfully committed.

    Inventory Service
          |
          v
      PostgreSQL
          |
        Outbox
          |
          v
        Kafka
          |
          +--------------------------+
          |                          |
          v                          v
    Query Model Consumer      Notification Consumer
          |                          |
          v                          v
      Couchbase             Notification PostgreSQL

The Query Model Consumer and Notification Consumer use independent Kafka consumer groups.

A failure or delay in one consumer does not prevent the other consumer from processing the same inventory event.

---

## 6. Inventory CQRS Architecture

The Inventory Service separates command and query responsibilities.

### 6.1 Command Side

The command side handles operations that modify inventory.

    Inventory Command
          |
          v
      PostgreSQL
          |
          +-------------------+
          |                   |
          v                   v
    Inventory State       Outbox Event

PostgreSQL is the authoritative source of inventory state.

Inventory changes and their corresponding Outbox events are written within the same PostgreSQL transaction.

### 6.2 Query Side

The query side handles inventory read operations.

    Customer
        |
        v
    Inventory Query API
        |
        v
    Couchbase

Couchbase contains a denormalized representation optimized for inventory queries.

The query model is updated asynchronously from inventory events and may temporarily lag behind the PostgreSQL source of truth.

Customer inventory read APIs do not query PostgreSQL directly.

---

## 7. Transactional Outbox

Inventory state changes and their corresponding events are written to PostgreSQL within the same transaction.

The transaction follows this general sequence:

    BEGIN TRANSACTION

        Update Inventory
              |
              +-- Update inventory state
              |
              +-- Create Outbox Event

    COMMIT

An independent Outbox Publisher continuously polls for pending Outbox events and publishes them to Kafka.

This ensures that a successfully committed inventory transaction does not lose its corresponding event if Kafka is temporarily unavailable.

If Kafka is unavailable, the inventory transaction can still succeed and the Outbox event remains pending until it can be published.

The Outbox Publisher uses bounded batch processing and bounded concurrency so that asynchronous processing does not overwhelm PostgreSQL or Kafka.

---

## 8. Event Processing

Inventory changes are propagated through Kafka to independent consumers.

### 8.1 Inventory Query Consumer

The Inventory Query Consumer:

- Consumes inventory update events.
- Updates the Couchbase inventory query model.
- Processes events independently of the Notification Consumer.
- Handles duplicate event delivery safely.

### 8.2 Notification Consumer

The Notification Consumer:

- Consumes inventory availability events.
- Identifies active back-in-stock notification requests.
- Creates user notifications.
- Completes the corresponding notification requests.
- Handles duplicate event delivery safely.

The Notification Consumer processes the notification request and notification state transactionally in PostgreSQL.

---

## 9. Data Ownership

Each service owns its persistent data.

Services must not directly access another service's database.

The primary ownership model is:

| Service | Storage | Purpose |
|---|---|---|
| User Service | PostgreSQL | User and authentication data |
| Inventory Service | PostgreSQL | Inventory source of truth and transactional data |
| Inventory Service | Couchbase | Inventory query model |
| Notification Service | PostgreSQL | Notification requests and notifications |
| Order Service | PostgreSQL | Order-related data |

The Order Service database is outside the primary scope of this demo because order creation and order lifecycle management are not implemented.

---

## 10. Resiliency Principles

The architecture is designed to tolerate failures of asynchronous components without compromising transactional inventory operations.

Key principles include:

- Inventory transactions do not depend on Kafka being immediately available.
- Pending Outbox events are retained until successfully published.
- Kafka consumers process events independently.
- Consumers are idempotent to tolerate duplicate event delivery.
- Inventory reduction requests are idempotent to tolerate client retries.
- Background processing uses bounded concurrency to avoid overwhelming dependent systems.
- Temporary downstream failures do not cause successfully committed inventory transactions to be rolled back.

---

## 11. Authentication and Authorization

The User Service is responsible for user registration and authentication and issues authentication tokens for authenticated users.

Protected services validate the authentication token when receiving requests.

The User Service is not used as a proxy for business requests.

For example:

    Customer
        |
        | Authentication Request
        v
    User Service
        |
        v
    Authentication Token


    Customer
        |
        | Protected Inventory Request
        | + Authentication Token
        v
    Inventory Service
        |
        +-- Validate Token
        |
        +-- Authorize Operation
        |
        v
    Inventory Operation

This keeps business services independently deployable and avoids making the User Service a runtime dependency for every protected API request.

---

## 12. Architectural Constraints

The following constraints apply to the system:

- The system supports a single store.
- PostgreSQL is the source of truth for inventory.
- Couchbase is used for the Inventory query model.
- Services do not directly access another service's database.
- Kafka is used for asynchronous event propagation.
- Inventory events are published using the Transactional Outbox pattern.
- Inventory commands and asynchronous event processing must be idempotent.
- Order creation and order lifecycle management are outside the scope of this system.
- External SMS, email, and push notification providers are outside the scope of this version.
- The User Service is not used as a proxy for business requests.
- Protected services validate authentication tokens independently.

---

## 13. Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Application development |
| Spring Boot | Service framework |
| PostgreSQL | Transactional data and source of truth |
| Couchbase | Inventory query model |
| Apache Kafka | Asynchronous event propagation |
| JWT | User authentication |
| Cucumber | Behavior-driven testing |
| Azure | Cloud deployment |