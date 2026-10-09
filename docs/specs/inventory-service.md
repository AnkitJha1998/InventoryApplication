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

Both query APIs must read from Couchbase and must not query PostgreSQL directly.

## Couchbase Query Model

Store one query document per product.

The document contains:

- `productId`
- `productName`
- `availableQuantity`
- `available`
- `version`

`productId` uniquely identifies the document.

`available` is true when `availableQuantity` is greater than zero.

`version` is a monotonically increasing inventory version for that product. Every committed inventory state change that generates an inventory event must have a version greater than the preceding version for that product.

The Inventory Service must not use Couchbase as the authoritative source for inventory writes.

## Inventory Event Contract

Inventory events are created in the same PostgreSQL transaction as the corresponding inventory state change, operation record, and Outbox record.

Events published to `inventory-updates` must contain the resulting inventory state, including:

- `eventId`
- `productId`
- `availableQuantity`
- `version`
- `occurredAt`

The event must carry the resulting stock quantity rather than only a stock delta.

The event schema must be consistent across all event producers and consumers.

## Inventory Query Consumer

The Inventory Query Consumer consumes events from `inventory-updates` and updates the corresponding Couchbase product document.

The consumer must:

- Create the document when a product has no existing query document.
- Apply an event only when its version is greater than the version currently stored.
- Ignore duplicate events and events with versions less than or equal to the stored version.
- Avoid overwriting newer query-model state with older events.
- Acknowledge a Kafka record only after its Couchbase update or safe rejection as stale or duplicate has completed.
- Retry transient failures without advancing the consumer offset past unprocessed records.

Couchbase updates must use an atomic conditional update or equivalent concurrency control so concurrent consumers cannot overwrite a newer version with an older one.

The consumer must be safe to retry and must not depend on exactly-once Kafka delivery.

## Query Consistency and Failure Handling

- PostgreSQL is the source of truth for inventory.
- Couchbase is an asynchronously updated read model.
- Query results may temporarily lag behind PostgreSQL.
- Couchbase failures must not roll back or block PostgreSQL inventory transactions.
- Kafka consumer failures must not cause unprocessed events to be silently skipped.
- Duplicate and out-of-order event delivery must not corrupt or regress the query model.