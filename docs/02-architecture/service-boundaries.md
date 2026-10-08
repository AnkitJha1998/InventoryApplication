\# Service Boundaries



\## 1. User Service



\### Responsibility



\- User registration.

\- User authentication.

\- Customer and Store Manager role management.

\- JWT token issuance.



\### Owns



\- User accounts.

\- Authentication data.

\- User roles.



\### Communication



\- Exposes registration and authentication APIs.

\- Does not proxy business requests to other services.



\---



\## 2. Inventory Service



\### Responsibility



\- Increase product inventory.

\- Reduce product inventory.

\- Validate inventory availability.

\- Handle inventory reduction idempotency.

\- Provide inventory queries.

\- Publish inventory events.



\### Owns



\- Inventory source of truth in PostgreSQL.

\- Inventory operation/idempotency data.

\- Transactional Outbox data.

\- Inventory query model in Couchbase.



\### Communication



\- Store Manager → Inventory Service: inventory updates.

\- Order Service → Inventory Service: inventory reduction.

\- Customer → Inventory Service: inventory queries.

\- Inventory Service → Kafka: inventory events.



The Inventory Service is the only service allowed to modify inventory state.



\---



\## 3. Order Service



\### Responsibility



For this system, the Order Service represents the external system that requests inventory reduction when an order requires inventory to be deducted.



Order creation and order lifecycle management are outside the scope of this demo.



\### Owns



\- Order-related data.



\### Communication



\- Order Service → Inventory Service: synchronous inventory reduction request.



The Order Service must not access the Inventory Service database.



\---



\## 4. Notification Service



\### Responsibility



\- Create back-in-stock notification requests.

\- Process inventory availability events.

\- Create customer notifications.

\- Retrieve notifications.

\- Mark notifications as read.



\### Owns



\- Notification requests.

\- Notifications.

\- Notification event-processing/idempotency data.



\### Communication



\- Customer → Notification Service: notification APIs.

\- Kafka → Notification Service: inventory availability events.



The Notification Service does not synchronously call the Inventory Service for restock processing.



\---



\## 5. Boundary Rules



\- Each service owns its business capabilities and persistent data.

\- Services must not directly access another service's database.

\- Inventory state can only be modified by the Inventory Service.

\- Inventory queries are served from the Inventory query model.

\- Order Service communicates with Inventory Service through REST.

\- Inventory events are propagated through Kafka.

\- Notification processing is triggered asynchronously through inventory events.

\- The User Service handles authentication and issues JWT tokens.

\- Protected services independently validate JWT tokens and authorize operations based on the user's role.

\- New service-to-service dependencies should not be introduced unless required by a defined business flow.

