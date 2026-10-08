# Architecture Overview

## 1. Purpose

This document defines the high-level architecture of the Retail Inventory System.

The system is a microservice-based application supporting:

- User management and authentication.
- Inventory management.
- Inventory querying.
- Simulated order creation.
- Back-in-stock notifications.

The system supports a single store.

---

## 2. Architectural Style

The system consists of independently deployable services with clear ownership boundaries.

The Inventory domain uses CQRS to separate inventory write operations from inventory read operations.

The architecture uses:

- REST for synchronous operations requiring an immediate response.
- Kafka for asynchronous event propagation.
- PostgreSQL for transactional and authoritative data.
- Couchbase for the Inventory query model.
- Transactional Outbox for reliable publication of inventory events.
- JWT for user authentication and authorization.

---

## 3. Services

### User Service

Responsible for:

- User registration.
- User authentication.
- Customer and Store Manager roles.
- JWT token issuance.

The User Service issues JWT authentication tokens.

### Inventory Service

Responsible for:

- Increasing inventory.
- Reducing inventory.
- Inventory availability validation.
- Inventory reduction idempotency.
- Inventory queries.

PostgreSQL is the source of truth for inventory.

Couchbase provides the Inventory query model.

Protected Inventory APIs validate the JWT before processing requests.

### Order Service

Responsible for providing the simulated order creation endpoint.

The Order Service receives an order request, validates the customer's JWT, and requests inventory reduction from the Inventory Service.

Order persistence and order lifecycle management are outside the scope of this system.

### Notification Service

Responsible for:

- Back-in-stock notification requests.
- Creating customer notifications.
- Retrieving notifications.
- Marking notifications as read.

The Notification Service uses PostgreSQL for persistent data.

---

## 4. High-Level Architecture

    Customer
        |
        +--------------------> User Service
        |                         |
        |                         +-- JWT
        |
        +--------------------> Order Service
        |                         |
        |                         | JWT
        |                         v
        |                    Inventory Service
        |                         |
        |                         +----> PostgreSQL
        |                         |       Source of Truth
        |                         |
        |                         +----> Couchbase
        |                                 Query Model
        |
        +--------------------> Notification Service
                                  |
                                  +----> PostgreSQL


    Store Manager
        |
        +--------------------> Inventory Service


    Inventory Service
        |
        | Transactional Outbox
        v
      Kafka
        |
        +--------------------> Inventory Query Consumer
        |                              |
        |                              v
        |                          Couchbase
        |
        +--------------------> Notification Consumer
                                       |
                                       v
                              Notification PostgreSQL

---

## 5. Major Architectural Decisions

### 5.1 Inventory Source of Truth

PostgreSQL is the authoritative source of inventory state.

All inventory modifications are performed through the Inventory Service.

Couchbase is a read model and is not an authoritative source for inventory changes.

### 5.2 CQRS

Inventory write operations and read operations are separated.

The command side writes to PostgreSQL.

The query side reads from Couchbase.

The Couchbase query model is updated asynchronously and may temporarily lag behind the PostgreSQL source of truth.

### 5.3 Transactional Outbox

Inventory changes and their corresponding events are written to PostgreSQL within the same transaction.

A background publisher publishes pending Outbox events to Kafka.

Therefore, a temporary Kafka failure does not cause a successfully committed inventory operation to lose its event.

### 5.4 Asynchronous Event Processing

Kafka is used to propagate inventory changes to independent consumers.

The Inventory Query Consumer updates Couchbase.

The Notification Consumer processes back-in-stock notification requests.

The consumers use independent consumer groups.

### 5.5 Idempotency

Inventory reduction requests are idempotent so that client retries do not reduce inventory more than once.

Asynchronous consumers are idempotent so that duplicate event delivery does not create duplicate effects.

### 5.6 Service Data Ownership

Each service owns its persistent data.

Services must not directly access another service's database.

Business communication between services occurs through defined REST APIs or Kafka events.

### 5.7 Authentication

The User Service is responsible for authentication and issuing JWT tokens.

Protected services independently validate JWT tokens.

Services authorize requests based on the authenticated user's role and the API being accessed.

When a protected service calls another protected service, the user's JWT is forwarded with the request.

The User Service is not used as a proxy for business requests.

---

## 6. Communication Model

Synchronous communication is used when the caller requires an immediate result.

Examples:

- Customer → User Service.
- Customer → Order Service.
- Store Manager → Inventory Service.
- Customer → Inventory Service for inventory queries.
- Customer → Notification Service.
- Order Service → Inventory Service.

Asynchronous communication is used when processing can occur independently of the original request.

The primary asynchronous flow is:

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
          +------------> Inventory Query Consumer
          |
          +------------> Notification Consumer

---

## 7. Repository and Build Structure

The services are maintained as separate Maven projects in the same Git repository. Each service has its own `pom.xml`, source tree, test suite, configuration, database migrations where applicable, build lifecycle, and deployable artifact.

There is no central or multi-module Maven parent project.

Services must not import one another's Java classes, repositories, entities, or implementation modules. Inter-service communication occurs only through the defined REST and Kafka boundaries.

New services added to this repository must follow the same independent-project structure.

Each service is started independently during local development:

- Inventory Service: port `8081`.
- Order Service: port `8080`.

---

## 8. Architectural Constraints

The following constraints apply to the system:

- The system supports a single store.
- PostgreSQL is the source of truth for inventory.
- Couchbase is used only as the Inventory query model.
- Services must not directly access another service's database.
- Inventory modifications can only be performed through the Inventory Service.
- Inventory events are published using the Transactional Outbox pattern.
- Inventory operations and asynchronous event processing must be idempotent.
- Order creation is limited to the simulated order flow and does not include order persistence or lifecycle management.
- External email, SMS, and push notification providers are outside the scope.
- No additional infrastructure should be introduced unless required by a defined system requirement.

---

## 9. Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Application development |
| Spring Boot | Service framework |
| Spring Security | Authentication and authorization |
| Spring Data JPA / Hibernate | PostgreSQL persistence |
| PostgreSQL | Transactional data |
| Couchbase | Inventory query model |
| Apache Kafka | Asynchronous event propagation |
| JWT | Authentication |
| Cucumber | BDD testing |
| Azure | Cloud deployment |