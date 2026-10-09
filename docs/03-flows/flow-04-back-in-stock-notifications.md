\# Flow 04 - Back-in-Stock Notifications



\## Purpose



Allow customers to request notifications when a product becomes available again.



\## Flow A - Customer Notification Request



Customer

&#x20;   |

&#x20;   | POST /notifications/back-in-stock

&#x20;   v

Notification Service

&#x20;   |

&#x20;   | Validate JWT and CUSTOMER role

&#x20;   | Validate productid and if the stock is actually 0, if not state product is available

&#x20;   | Prevent duplicate active requests

&#x20;   v

Notification PostgreSQL

&#x20;   |

&#x20;   | Persist notification request

&#x20;   v

Return 201 Created



\## Flow B - Inventory Becomes Available



Inventory Service

&#x20;   |

&#x20;   | Inventory quantity changes from 0 to positive

&#x20;   v

PostgreSQL Transaction

&#x20;   |

&#x20;   | Update inventory quantity and version

&#x20;   | Create NotificationTrigger Outbox event

&#x20;   v

Outbox Publisher

&#x20;   |

&#x20;   | Publish event

&#x20;   v

Kafka: NotificationTrigger

&#x20;   |

&#x20;   v

Notification Service processes the trigger in Flow 5



\## Rules



\- Only CUSTOMER can create a notification request.

\- Derive customer identity from the validated JWT.

\- Prevent duplicate active requests for the same customer and product.

\- Persist notification requests in Notification Service PostgreSQL.

\- Generate NotificationTrigger only when existing inventory transitions from zero to positive.

\- Persist the inventory update and Outbox event in the same PostgreSQL transaction.

\- Reuse the existing Outbox publishing mechanism.

\- Use the exact Kafka topic name NotificationTrigger.

\- Only allow for notification request if stock is 0.

\- Flow 4 does not send notifications; delivery is handled by Flow 5.

