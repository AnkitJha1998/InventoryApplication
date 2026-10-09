# Flow 3.5 - Implementation Tasks



## Task 1: Inventory Addition API



* [ ] Implement `POST /inventory/increase`.

* [ ] Validate product ID and positive quantity.

* [ ] Enforce `STORE_MANAGER` authorization in Inventory Service.

* [ ] Verify `CUSTOMER` requests return `403 Forbidden`.

* [ ] Verify missing or invalid JWTs return `401 Unauthorized`.

* [ ] Ensure rejected requests cause no database or Outbox side effects.



## Task 2: PostgreSQL Inventory Versioning



* [ ] Add or verify the inventory version field on the PostgreSQL inventory record.

* [ ] Ensure every successful inventory addition or reduction that emits an event increments the version exactly once.

* [ ] Ensure quantity and version updates are atomic.

* [ ] Protect concurrent updates to the same product against conflicting versions.

* [ ] Preserve existing inventory reduction idempotency behavior.



## Task 3: Transactional Outbox



* [ ] Persist the version in the Outbox event.

* [ ] Ensure the persisted version matches the PostgreSQL inventory version.

* [ ] Persist inventory state, version, applicable operation record, and Outbox event in one transaction.

* [ ] Publish the persisted event to `InventoryUpdate`.

* [ ] Ensure retries preserve the event ID, quantity, and version.

* [ ] Verify failed transactions do not leave orphaned Outbox events.



## Task 4: Couchbase Consumer



* [ ] Consume versioned events from `InventoryUpdate`.

* [ ] Store the applied version in each Couchbase inventory document.

* [ ] Apply events only when their version is newer than the stored version.

* [ ] Make version comparison and document mutation atomic.

* [ ] Handle duplicate, stale, and out-of-order events.

* [ ] Ensure transient Couchbase failures do not silently lose events.



## Task 5: Automated Tests



* [ ] Test successful inventory addition by `STORE_MANAGER`.

* [ ] Test inventory addition rejection for `CUSTOMER`.

* [ ] Test unauthenticated and invalid-token requests.

* [ ] Test invalid quantities and missing products.

* [ ] Test inventory addition and reduction version increments.

* [ ] Test transactional rollback of inventory and Outbox changes.

* [ ] Test concurrent updates to the same product.

* [ ] Test Outbox publication retry behavior.

* [ ] Test Couchbase duplicate, stale, and out-of-order event handling.

* [ ] Test the complete PostgreSQL → Outbox → Kafka → Couchbase pipeline.



## Completion Criteria



Flow 3.5 is complete when inventory addition is authorized correctly, inventory changes have authoritative PostgreSQL versions, events are reliably published to `InventoryUpdate`, and Couchbase cannot regress when duplicate or out-of-order events arrive.



