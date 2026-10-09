# Flow 3.5 - Inventory Addition and CQRS Synchronization



## Purpose



Implement inventory addition and complete the versioned event pipeline that synchronizes PostgreSQL inventory changes to the Couchbase query model.



## Scope



* Inventory addition API.

* `STORE_MANAGER` authorization.

* Rejection of `CUSTOMER` inventory addition requests.

* PostgreSQL as the authoritative inventory store and version source.

* Atomic inventory version increments.

* Versioned `InventoryUpdate` events persisted through the transactional Outbox.

* Asynchronous publication to `InventoryUpdate`.

* Version-aware Couchbase synchronization.

* Duplicate, stale, and out-of-order event handling.

* End-to-end verification of inventory additions and reductions.



## Flow



```

STORE_MANAGER

&#x20;    |

&#x20;    | POST /inventory/increase

&#x20;    v

Inventory Service

&#x20;    |

&#x20;    | Validate JWT, role, and request

&#x20;    v

PostgreSQL Transaction

&#x20;    |

&#x20;    +--> Update inventory quantity

&#x20;    +--> Increment inventory version

&#x20;    +--> Record operation as applicable

&#x20;    +--> Persist InventoryUpdate in Outbox

&#x20;    |

&#x20;    v

Transaction Commit

&#x20;    |

&#x20;    v

Outbox Publisher

&#x20;    |

&#x20;    | Publish persisted event

&#x20;    v

Kafka: InventoryUpdate

&#x20;    |

&#x20;    v

Inventory Query Consumer

&#x20;    |

&#x20;    | Apply only a newer version

&#x20;    v

Couchbase Query Model

&#x20;    |

&#x20;    v

Inventory Query APIs

```



## Processing Rules



### Inventory Addition



* Inventory Service exposes the inventory addition endpoint.

* Only `STORE_MANAGER` may add inventory.

* `CUSTOMER` requests must be rejected with `403 Forbidden`.

* Missing or invalid JWTs must be rejected with `401 Unauthorized`.

* Quantity must be greater than zero.

* Inventory addition must update PostgreSQL, not Couchbase.

* Rejected requests must not change inventory, increment its version, or create an Outbox event.



### Inventory Versioning



* PostgreSQL is authoritative for both inventory quantity and version.

* Each product has a monotonically increasing inventory version.

* Each successful inventory quantity change that generates an `InventoryUpdate` increments the version exactly once.

* Quantity and version changes must be atomic.

* Concurrent updates to the same product must not generate conflicting versions.

* Inventory state, applicable operation records, and Outbox events must be committed in the same PostgreSQL transaction.

* A failed transaction must not leave partial state or an orphaned event.



### Outbox and Kafka



* Persist the `InventoryUpdate` event, including its version, in the Outbox transaction.

* Publish the persisted event to the exact topic `InventoryUpdate`.

* The publisher must not generate or change inventory versions.

* Retries must preserve the event ID, product ID, resulting quantity, and version.

* Kafka unavailability must not lose committed inventory changes or pending Outbox events.

* Delivery is at least once; duplicate delivery is expected.



### Couchbase Synchronization



* The consumer reads `InventoryUpdate` events from `InventoryUpdate`.

* Couchbase is a read model; PostgreSQL remains the source of truth.

* Apply an incoming event only if its version is newer than the stored version.

* Duplicate, stale, or out-of-order events must not regress inventory state.

* Version comparison and document update must be atomic.

* Retain the applied version in each Couchbase inventory document.

* A consumer must not acknowledge an event before the update succeeds or the event is safely classified as duplicate or stale.

* Query APIs continue reading from Couchbase.



## Verification



* A `STORE_MANAGER` can add inventory.

* A `CUSTOMER` cannot add inventory.

* Missing or invalid JWTs are rejected.

* Invalid quantities are rejected without side effects.

* Inventory additions and reductions update PostgreSQL and increment versions correctly.

* The committed PostgreSQL version matches the version in the Outbox event.

* Inventory state and its Outbox event roll back together on transaction failure.

* Concurrent changes to the same product receive distinct, correctly ordered versions.

* Outbox retries preserve the original event ID and version.

* Events are published to `InventoryUpdate`.

* Couchbase reflects both additions and reductions.

* Duplicate, stale, and out-of-order events cannot overwrite newer state.

* A Couchbase failure does not silently lose an event.

* The inventory query APIs return the synchronized quantity and version.



## Out of Scope



* Notification triggers and back-in-stock processing.

* Notification delivery.

* Full order persistence and lifecycle management.

* Replacing the transactional Outbox with CDC or Debezium.
