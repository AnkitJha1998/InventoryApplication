\# Flow 3.5.1 - Inventory Creation



\## Purpose



Add inventory creation and ensure the initial inventory state is synchronized to Couchbase using the existing versioned event pipeline.



\## Flow



```

STORE\_MANAGER

&#x20;    |

&#x20;    | Create Inventory

&#x20;    v

Inventory Service

&#x20;    |

&#x20;    v

PostgreSQL Transaction

&#x20;    |

&#x20;    +--> Create inventory record

&#x20;    +--> Initialize inventory version

&#x20;    +--> Create Outbox event

&#x20;    |

&#x20;    v

Outbox Publisher

&#x20;    |

&#x20;    v

Kafka: InventoryUpdates

&#x20;    |

&#x20;    v

Inventory Query Consumer

&#x20;    |

&#x20;    v

Couchbase

```



\## Rules



\* Only `STORE\_MANAGER` can create inventory.

\* Prevent duplicate inventory records for the same product.

\* Initialize the inventory version consistently with Flow 3.5.

\* Persist the inventory record and Outbox event in the same transaction.

\* Include the initial quantity and version in the `InventoryUpdate` event.

\* Reuse the existing Outbox Publisher and Couchbase consumer.

\* Do not introduce notification processing in this flow.



\## Verification



\* Inventory creation succeeds for `STORE\_MANAGER`.

\* Unauthorized requests are rejected.

\* Duplicate creation is prevented.

\* The initial version is persisted and included in the Outbox event.

\* Couchbase receives the initial inventory state through `InventoryUpdates`.



