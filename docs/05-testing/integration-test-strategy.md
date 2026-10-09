# Integration Test Strategy

## Purpose

Integration tests for the Inventory Application will be implemented with Cucumber after the individual flows are implemented. This document records the required integration-test scope so it is not lost between flow implementations.

## Location and tooling

- Feature files will live under each independent service project in `src/test/resources/features/`.
- Step definitions and test orchestration will live under each service project's `src/test/java/` test packages.
- Cucumber is the acceptance-test framework defined by the architecture documentation.
- Tests must run against real service boundaries and must not replace inter-service REST, PostgreSQL, Kafka, or Couchbase behavior with mocks for end-to-end scenarios.
- Service projects remain independently buildable; integration tests may start the required services and infrastructure through the development profile or an approved test environment.

## Flow 1: Order creation and inventory reduction

Required scenarios:

- A valid order reduces inventory through Order Service and Inventory Service.
- Inventory reduction, operation persistence, and Outbox persistence commit atomically.
- Insufficient inventory returns the agreed `400` response.
- Repeating the same operation does not reduce inventory a second time.
- Repeating the same idempotency key with a different quantity is rejected.
- Concurrent reductions cannot create negative inventory.
- Kafka unavailability does not roll back a committed inventory reduction.
- A pending Outbox event is retried and eventually published.

## Flow 2: User authentication and JWT integration

Required scenarios:

- A CUSTOMER can register and log in.
- A STORE_MANAGER can register and log in.
- Passwords are persisted as hashes and are never returned.
- Login returns a JWT containing `userId`, `role`, `iat`, and `exp`.
- Missing, malformed, invalid, or expired JWTs return `401`.
- A CUSTOMER can call `POST /orders`.
- A STORE_MANAGER cannot call `POST /orders` and receives `403`.
- Order Service forwards the incoming Authorization header unchanged to Inventory Service.
- Inventory Service independently validates the forwarded JWT.
- Both CUSTOMER and STORE_MANAGER can call `POST /inventory/reduce` when the token is valid; reduction has no role restriction.

## Flow 3: Inventory query and Couchbase read model

Required scenarios:

- A valid CUSTOMER can query one product.
- A valid STORE_MANAGER can query one product.
- A valid CUSTOMER can browse the paginated catalogue.
- A valid STORE_MANAGER can browse the paginated catalogue.
- Missing query documents return `404`.
- Invalid pagination returns `400`.
- Query responses are read from Couchbase and do not access PostgreSQL.
- An InventoryUpdate Kafka event creates a missing Couchbase document.
- A duplicate InventoryUpdate event does not create an incorrect duplicate state.
- A repeated InventoryUpdate event safely applies the same product state without creating duplicates.
- A Couchbase failure does not roll back a committed Inventory command operation.
- A Kafka consumer failure allows the event to be redelivered safely.
- Catalogue results are ordered by most recently updated.

## Flow 3.5.1: Inventory creation

The Flow 3.5.1 implementation must add scenarios for:

- A STORE_MANAGER can create inventory and receives HTTP 200 with version `1`.
- A CUSTOMER receives `403` for inventory creation.
- Missing, malformed, invalid, or expired JWTs receive `401`.
- Missing or invalid product fields and non-positive quantity receive `400` without side effects.
- A duplicate product receives `400` without changing inventory or creating an additional event.
- The initial inventory row and its single `InventoryUpdate` Outbox event commit atomically.
- A transaction failure rolls back both the new inventory row and its Outbox event.
- The first event creates a Couchbase document with version `1` and the initial available quantity.

## Flow 3.5: Versioned inventory events and increase

The Flow 3.5 implementation must add scenarios for:

- Each successful reduction or manager increase receives exactly one monotonically increasing product version.
- The resulting version, stable event ID, product details, quantity, and occurrence time are included in the InventoryUpdate event.
- A newer version replaces an older Couchbase document version.
- An equal version is ignored as a duplicate and an older version is ignored as stale.
- A missing Couchbase document is safely created by a newer event.
- A transient Couchbase failure does not acknowledge the Kafka message.
- A STORE_MANAGER can increase positive inventory and a CUSTOMER receives `403`.
- Missing products and invalid or missing quantities return the documented error without changing PostgreSQL or Outbox state.
- Replaying a reduction with the same `orderId` and `productId` returns the original result without another version or event.
- Query results remain ordered by `updatedAt DESC` with a stable `productId` tie-breaker.
- Concurrent inventory changes produce correctly ordered versions.

NotificationTrigger remains out of scope for Flow 3.5 and is reserved for Flow 4.

## Flow 4: Back-in-stock notifications

Required scenarios:

- A CUSTOMER can submit `POST /notifications/back-in-stock` with only `productId`; the customer ID is always derived from the validated JWT.
- Missing, malformed, invalid, or expired JWTs return `401`, while a STORE_MANAGER receives `403`.
- The request forwards the exact Authorization header to `GET /inventory/{productId}`.
- Missing products and positive stock return `400`; downstream query failures return `500`.
- A zero-quantity product creates one persisted ACTIVE request and returns `201` with its request ID.
- A duplicate active request for the same customer and product returns `400`; customer identity is isolated between JWTs.
- A zero-to-positive inventory transition emits exactly one `NotificationTrigger` event; creation and positive-to-positive changes emit none.
- The trigger contains event ID, product ID, product name, available quantity, version, and event timestamp on the exact `NotificationTrigger` topic.
- A trigger with no active requests is idle and acknowledged without creating a notification.
- A trigger completes every active request for the product and prepares one notification per request with exact message `<Item Name> is now available`.
- Duplicate event IDs are idempotent and do not create duplicate notifications.
- Notification request, notification, and processed-event writes commit atomically; a database failure leaves the Kafka message unacknowledged for redelivery.

## Test data and cleanup

- User IDs must be generated as random UUIDs.
- Test users, products, PostgreSQL records, Kafka records, and Couchbase documents must be isolated per scenario or cleaned up after each scenario.
- Tests must not depend on execution order.
- Manual development inventory seed data is not a substitute for automated integration-test fixtures.
