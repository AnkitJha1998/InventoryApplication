\# Flow 03 - Inventory Query



\## Purpose



Allow customers to retrieve inventory availability for a product and browse the product catalogue using a read-optimized query model.



\## Query Flow



&#x20;   Customer

&#x20;       |

&#x20;       | GET inventory API

&#x20;       v

&#x20;   Inventory Service

&#x20;       |

&#x20;       v

&#x20;   Couchbase

&#x20;       |

&#x20;       v

&#x20;   Customer



\## Read Model Update Flow



&#x20;   Inventory Command

&#x20;       |

&#x20;       v

&#x20;   PostgreSQL

&#x20;       |

&#x20;       +--> Inventory updated

&#x20;       +--> Outbox event created

&#x20;       |

&#x20;       v

&#x20;   Outbox Publisher

&#x20;       |

&#x20;       v

&#x20;   Kafka: inventory-updates

&#x20;       |

&#x20;       v

&#x20;   Inventory Query Consumer

&#x20;       |

&#x20;       v

&#x20;   Couchbase



\## Key Rules



\- PostgreSQL remains the authoritative source of inventory state.

\- Couchbase stores the read-optimized inventory query model.

\- Inventory query APIs must read from Couchbase, not PostgreSQL.

\- Inventory events are published through the existing Transactional Outbox.

\- The Inventory Query Consumer updates Couchbase asynchronously.

\- Inventory writes must not wait for Couchbase updates.

\- Events contain the resulting inventory state and a monotonically increasing version for the product.

\- The consumer must prevent duplicate or older events from overwriting newer query-model state.

\- Inventory queries support retrieving a product and browsing the catalogue with pagination.

\- The system supports a single store; no `storeId` is required.

\- The query model may temporarily lag behind PostgreSQL.



\## Failure Handling



\- If Couchbase is unavailable, inventory queries may fail temporarily. PostgreSQL inventory writes must remain independent of Couchbase availability.

\- If the consumer fails, Kafka must retain unprocessed records according to the configured retention and consumer-offset behavior.

\- Retried or duplicate events must not corrupt or regress the query model.



\## Out of Scope



\- Inventory creation or modification.

\- Inventory reduction and idempotency, covered by Flow 1.

\- Back-in-stock notification request management, covered by Flow 4.

\- Back-in-stock notification processing, covered by Flow 5.

