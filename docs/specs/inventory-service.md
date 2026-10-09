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

## Inventory Query APIs

### Get Product Inventory

`GET /inventory/{productId}`

- Requires a valid JWT.
- Allows `CUSTOMER` and `STORE_MANAGER` roles.
- Reads the product inventory document from Couchbase.
- Returns `404 Not Found` if the product is absent from the query model.

### Browse Product Catalogue

`GET /inventory?page=0&size=20`

- Requires a valid JWT.
- Allows `CUSTOMER` and `STORE_MANAGER` roles.
- Returns a paginated product catalogue from Couchbase.
- Uses zero-based pagination.
- Defaults to page `0` and size `20`.
- Enforces a maximum page size of `100`.

Both query APIs must read from Couchbase and must not query PostgreSQL directly. The Couchbase bucket requires a query index (for local development, run `CREATE PRIMARY INDEX ON inventory USING GSI;`) so Spring Data pagination and updated-at ordering can execute.

## Couchbase Query Model

Store one query document per product.

The document contains:

- `productId`
- `productName`
- `productType`
- `availableCount` (also exposed as `availableQuantity`)
- `available` (derived from quantity > 0)
- `updatedAt`

`productId` uniquely identifies the document. `updatedAt` is populated from the event `occurredAt` timestamp and is used for catalogue ordering.

The Inventory Service must not use Couchbase as the authoritative source for inventory writes.

## Inventory Event Contract

Inventory events are created in the same PostgreSQL transaction as the corresponding inventory state change, operation record, and Outbox record.

Events published to the exact `InventoryUpdate` topic must contain the resulting inventory state, including:

- `eventId`
- `productId`
- `productName`
- `productType`
- `currentCount`
- `occurredAt`

The event must carry the resulting stock quantity rather than only a stock delta.

The event schema must be consistent across all event producers and consumers. Inventory addition and event versioning are deferred to Flow 3.5.

## Inventory Query Consumer

The Inventory Query Consumer consumes events from `InventoryUpdate` in its independent consumer group and updates the corresponding Couchbase product document.

The consumer must:

- Create or update the document when a product has no existing query document or when an event is redelivered.
- Acknowledge a Kafka record only after its Couchbase write has completed successfully.
- Retry transient failures without advancing the consumer offset past unprocessed records.

The consumer must be safe to retry and must not depend on exactly-once Kafka delivery.

## Query Consistency and Failure Handling

- PostgreSQL is the source of truth for inventory.
- Couchbase is an asynchronously updated read model.
- Query results may temporarily lag behind PostgreSQL.
- Couchbase failures must not roll back or block PostgreSQL inventory transactions.
- Kafka consumer failures must not cause unprocessed events to be silently skipped.
- Duplicate event delivery is safe because the product ID is the Couchbase document key. Event versioning and stale-event suppression are deferred to Flow 3.5.



## Inventory Addition

### Endpoint

`POST /inventory/increase`

Authentication: JWT

Authorization: `STORE_MANAGER`

Request:

```
{
  "productId": "P1001",
  "quantity": 10
}
```

Success:

* Return `200 OK` when inventory is increased successfully.
* Return the resulting inventory quantity and version.

Failure responses:

* `400 Bad Request` — invalid product ID or quantity.
* `401 Unauthorized` — missing or invalid JWT.
* `403 Forbidden` — authenticated user does not have the `STORE_MANAGER` role.
* `404 Not Found` — product does not exist.

The exact response DTO must follow the project's existing API conventions.

### Authorization Rules

* Inventory Service must enforce authorization independently.
* Only `STORE_MANAGER` may invoke inventory addition.
* `CUSTOMER` must not be allowed to add inventory.
* Authorization failure must not modify PostgreSQL, increment inventory version, or create an Outbox event.

## Inventory Versioning

* PostgreSQL is the authoritative source for inventory quantity and version.
* Each product inventory record maintains a monotonically increasing version.
* Every successful inventory quantity change that emits an `InventoryUpdate` increments the version exactly once.
* Inventory quantity and version must be changed atomically.
* Concurrent updates to the same product must be serialized or protected by a concurrency-control strategy that prevents conflicting versions.
* Use the existing Spring Data JPA and Hibernate persistence approach.
* Do not let the Outbox Publisher generate inventory versions.

## InventoryUpdate Outbox Event

Every successful inventory addition or reduction must persist an `InventoryUpdate` event in the same PostgreSQL transaction as the inventory change.

The event contains:

* `eventId`
* `productId`
* `availableQuantity` — resulting quantity after the change
* `version` — authoritative inventory version
* `eventTimestamp`

The event's version must match the version committed with the corresponding inventory update.

The Outbox Publisher publishes the persisted event to `InventoryUpdates`. Retries must preserve the event ID and version.

The Outbox event must not be published if its inventory transaction rolls back.

## Couchbase Version-Aware Updates

* The Inventory query consumer reads `InventoryUpdate` events from `InventoryUpdates`.
* Couchbase stores the inventory quantity and the last applied version.
* An event may update a document only if its version is greater than the document's current version.
* Version comparison and document mutation must be atomic.
* Duplicate and stale events must not change the document.
* Out-of-order events must not overwrite newer state.
* Consumer processing must tolerate at-least-once delivery.
* Query APIs continue reading the Couchbase query model, not PostgreSQL.

## Flow 3.5 Constraints

* PostgreSQL remains the source of truth.
* Couchbase is used only as the query model in this flow.
* Inventory addition and reduction use the same versioning rules.
* Inventory update, version increment, applicable operation record, and Outbox event creation must participate in the same transaction.
* Notification processing is not implemented in this flow.

## Inventory Creation

* Provide an inventory creation API for `STORE_MANAGER`.
* Prevent duplicate inventory records for the same product.
* Initialize the inventory version using the Flow 3.5 versioning convention.
* Persist the inventory record and initial `InventoryUpdate` Outbox event in one PostgreSQL transaction.
* Publish through the existing `InventoryUpdates` pipeline.
* Reuse the existing version-aware Couchbase consumer.

