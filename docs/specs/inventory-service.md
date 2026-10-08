# Inventory Service Specification

## Responsibility

The Inventory Service owns inventory state and is the only service allowed to modify inventory.

## API

### Reduce Inventory

`POST /inventory/reduce`

Request:

    {
      "orderId": "O1001",
      "productId": "P1001",
      "quantity": 5
    }

The operation returns the result of the inventory reduction.

## Data

The Inventory Service uses PostgreSQL for:

- Inventory state.
- Inventory operation/idempotency records.
- Transactional Outbox events.

The service uses Couchbase only for the Inventory query model. Couchbase is not used in Flow 1.

## Persistence

The Inventory Service uses Spring Data JPA with Hibernate for PostgreSQL persistence.

Use JPA entities and Spring Data JPA repositories for:

- Inventory state.
- Inventory operation/idempotency records.
- Transactional Outbox records.

Inventory business operations should use the service layer and repositories rather than accessing PostgreSQL through JDBC directly.

The inventory update, operation record, and Outbox event must execute within a single Spring-managed transaction.

## Inventory Reduction Rules

- `quantity` must be greater than zero.
- Inventory must not become negative.
- `orderId + productId` identifies an inventory reduction operation.
- Repeating the same operation must not reduce inventory again.
- The same operation with a different quantity must be rejected.
- Inventory update, operation record, and Outbox event must be committed in one PostgreSQL transaction.
- Kafka publication happens asynchronously through the Outbox.

## Events

A successful inventory reduction creates an Outbox event containing the inventory change.

The event is published asynchronously to Kafka.

## Constraints

- The service must not directly access another service's database.
- PostgreSQL is the source of truth for inventory.
- Couchbase must not be used to perform inventory writes.