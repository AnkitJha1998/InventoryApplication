# Data and Event Architecture

## 1. Data Ownership

Each service owns its persistent data.

| Service | Storage | Purpose |
|---|---|---|
| User Service | PostgreSQL | User and authentication data |
| Inventory Service | PostgreSQL | Inventory source of truth |
| Inventory Service | Couchbase | Inventory query model |
| Notification Service | PostgreSQL | Notification requests and notifications |
| Order Service | PostgreSQL | Order-related data |

Services must not directly access another service's database.

---

## 2. Inventory Data Model

The Inventory Service uses CQRS.

PostgreSQL is the authoritative source of inventory state and is used by the command side.

Couchbase contains the read-optimized Inventory query model and is used by the query side.

The Couchbase model is updated asynchronously from inventory events and may temporarily lag behind PostgreSQL.

---

## 3. Transactional Outbox

Inventory state changes and their corresponding events are written to PostgreSQL within the same transaction.

The Outbox Publisher asynchronously publishes pending Outbox events to Kafka.

If Kafka is unavailable, the inventory transaction can still succeed and the Outbox event remains pending until it can be published.

Outbox processing uses bounded batch processing and bounded concurrency.

---

## 4. Kafka Events

Inventory changes are propagated asynchronously through Kafka.

The system uses two logical destinations:

| Destination | Consumer | Purpose |
|---|---|---|
| `inventory-updates` | Inventory Query Consumer | Update Couchbase |
| `notification-triggers` | Notification Consumer | Process back-in-stock notifications |

The same logical inventory change may produce records for both destinations.

The consumers use independent consumer groups.

The detailed event schema is defined in the Inventory Service specification.

---

## 5. Event Idempotency

Kafka and Outbox processing use at-least-once delivery semantics.

Consumers must therefore be idempotent.

The Notification Service records processed event IDs so that duplicate inventory events do not create duplicate notifications.

The Inventory Query Consumer must also safely handle duplicate events without producing an incorrect query model.

---

## 6. Consistency

Inventory writes are transactionally consistent within PostgreSQL.

The Couchbase Inventory query model is eventually consistent with PostgreSQL.

Back-in-stock notifications are processed asynchronously and may be delayed when Kafka or downstream consumers are unavailable.

---

## 7. Architectural Constraints

- PostgreSQL is the source of truth for inventory.
- Couchbase is a read model and cannot modify inventory.
- Inventory state and its Outbox event are committed in the same transaction.
- Kafka delivery is treated as at-least-once.
- Consumers must be idempotent.
- A restock notification is triggered only when stock changes from zero to positive.
- Services must not directly access another service's database.


## 8. Versioned Inventory Events

### Source of Truth

PostgreSQL owns the authoritative inventory quantity and inventory version.

Each successful inventory change that generates an inventory update event must increment the product's version atomically with the quantity update.

### Transactional Outbox

The inventory transaction must persist:

* The updated inventory quantity and version.
* Any applicable inventory operation record.
* The corresponding Outbox event.

These changes must commit or roll back together.

The persisted Outbox event must include the inventory version. If the Outbox schema stores a separate version column in addition to the serialized event payload, both representations must contain the same version and be written in the same transaction.

The Outbox Publisher publishes the persisted event; it does not generate versions.

### InventoryUpdates

The `InventoryUpdates` topic carries versioned inventory changes to the Inventory query consumer.

The event includes the event ID, product ID, resulting available quantity, version, and event timestamp.

### Couchbase Read Model

The Inventory query consumer applies an event only when its version is newer than the version currently stored in Couchbase for that product.

Version comparison and document update must be atomic. Duplicate, stale, and out-of-order events must not regress the query model.

Kafka delivery is at least once, so the consumer must be idempotent.

### Scope Boundary

Flow 3.5 implements inventory addition and version-aware synchronization of inventory additions and reductions. Notification processing is specified separately in Flow 4.
