
# Flow 03 - Inventory Query

## Purpose

Allow customers to retrieve inventory availability for a product and browse the product catalogue using a read-optimized query model.

## Query Flow

    Customer
       |
       | GET inventory API
       v
    Inventory Service
       |
       v
    Couchbase
       |
       v
    Customer

## Read Model Update Flow

    Inventory Command
       |
       v
    PostgreSQL
       |
       +--> Inventory updated
       +--> Outbox event created
       |
       v
    Outbox Publisher
       |
       v
    Kafka: InventoryUpdate
       |
       v
    Inventory Query Consumer
       |
       v
    Couchbase

## Key Rules

- PostgreSQL remains the authoritative source of inventory state.
- Couchbase stores the read-optimized inventory query model.
- Inventory query APIs must read from Couchbase, not PostgreSQL.
- Inventory events are published through the existing Transactional Outbox on the exact `InventoryUpdate` topic.
- The Inventory Query Consumer updates Couchbase asynchronously in an independent consumer group.
- Inventory writes must not wait for Couchbase updates.
- Events contain the resulting inventory state, product name, and `occurredAt` timestamp.
- Duplicate events are safe to process; event versioning and stale-event suppression are deferred to Flow 3.5.
- Inventory queries support retrieving a product and browsing the catalogue with pagination.
- The system supports a single store; no `storeId` is required.
- The query model may temporarily lag behind PostgreSQL.

## Failure Handling

- If Couchbase is unavailable, inventory queries may fail temporarily. PostgreSQL inventory writes must remain independent of Couchbase availability.
- If the consumer fails, Kafka must retain unprocessed records according to the configured retention and consumer-offset behavior.
- Retried or duplicate events must be safe to process and must not create duplicate documents.

## Out of Scope

- Inventory creation or modification.
- Inventory reduction and idempotency, covered by Flow 1.
- Inventory addition and event versioning, deferred to Flow 3.5.
- Back-in-stock notification request management, covered by Flow 4.
- Back-in-stock notification processing, covered by Flow 5.