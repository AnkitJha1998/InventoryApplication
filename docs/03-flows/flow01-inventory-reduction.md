# Flow 01 - Order Creation and Inventory Reduction

## Purpose

Simulate order creation and reduce product inventory as part of the order flow.

## Flow

    Consumer
        |
        | POST /orders
        v
    Order Service
        |
        | POST /inventory/reduce
        v
    Inventory Service
        |
        v
    PostgreSQL
        |
        +--> Inventory updated
        +--> Operation recorded
        +--> Outbox event created
        |
        v
    Inventory Service
        |
        v
    Order Service
        |
        v
    Consumer

    Outbox Publisher
        |
        v
      Kafka

## Key Rules

- Order Service exposes the consumer-facing order creation endpoint.
- Order Service requests inventory reduction from Inventory Service.
- Inventory Service is the only service allowed to modify inventory.
- Inventory cannot become negative.
- Inventory reduction is idempotent using `orderId + productId`.
- Repeating the same operation must not reduce inventory again.
- Inventory update, operation record, and Outbox event are committed in the same transaction.
- Kafka publication is asynchronous.
- A Kafka failure must not roll back a successfully committed inventory reduction.

## Out of Scope

- Full order persistence.
- Order lifecycle management.
- Kafka consumer processing.
- Couchbase query model updates.
- Back-in-stock notification processing.