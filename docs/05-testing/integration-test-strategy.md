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
- A repeated InventoryUpdate event safely overwrites the same product document without creating duplicates; stale-event suppression is deferred to Flow 3.5.
- A Couchbase failure does not roll back a committed Inventory command operation.
- A Kafka consumer failure allows the event to be redelivered safely.
- Catalogue results are ordered by most recently updated.

## Future Flow 3.5: Versioned inventory events

When versioning is introduced, add scenarios for:

- Each authoritative inventory change receives a monotonically increasing product version.
- The resulting version is included in the InventoryUpdate event.
- A newer version replaces an older Couchbase document version.
- An equal version is ignored as a duplicate.
- An older version is ignored as stale.
- Concurrent inventory changes produce correctly ordered versions.

## Future inventory increase flow

When the inventory increase API is defined, add scenarios for:

- Only an authorized STORE_MANAGER can increase inventory.
- A CUSTOMER cannot call inventory modification endpoints that require manager authorization.
- An inventory increase creates an InventoryUpdate event.
- A transition from zero to positive stock creates a NotificationTriggers event.
- Query and notification consumers remain independent.

## Test data and cleanup

- User IDs must be generated as random UUIDs.
- Test users, products, PostgreSQL records, Kafka records, and Couchbase documents must be isolated per scenario or cleaned up after each scenario.
- Tests must not depend on execution order.
- Manual development inventory seed data is not a substitute for automated integration-test fixtures.
