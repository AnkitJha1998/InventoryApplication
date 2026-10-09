# Flow 1 inventory reduction

Java 21 / Spring Boot implementation of the synchronous order-to-inventory reduction flow. `inventory-service` and `order-service` are independent Maven projects in the same Git repository; there is no central Maven parent project.

## Modules

* `order-service` (port 8080): exposes `POST /orders` and delegates synchronously to inventory.
* `inventory-service` (port 8081): exposes inventory command/query APIs, owns PostgreSQL state, Couchbase read model, Kafka consumer, and transactional outbox worker.
* `user-service` (port 8082): exposes user registration/login and issues JWTs.

The inventory operation is atomic: stock is conditionally decremented, the `(orderId, productId)` operation is recorded, and an `InventoryUpdate` outbox event is committed in the same PostgreSQL transaction. A `NotificationTriggers` event is added only for a `previous_count = 0` to `current_count > 0` transition. Retries with the same quantity are no-ops; a different quantity for the same key is a 400 conflict. Outbox delivery is asynchronous and does not roll back a successful reduction. The worker uses `PROCESSING` leases, `FOR UPDATE SKIP LOCKED`, and one initial attempt plus four retries; expired leases are reclaimed and exhausted leases become `FAILED`.

## Run locally

Prerequisites: JDK 21, Maven, and Docker Compose.

```shell
docker compose up -d
cd inventory-service
mvn clean verify
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# in another terminal, from the repository root
cd order-service
mvn clean verify
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# in another terminal, from the repository root
cd user-service
mvn clean verify
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Local development defaults are PostgreSQL `localhost:5432`, database `inventory`, user/password `postgres/postgres`, Kafka `localhost:9092`, Couchbase `couchbase://localhost` (`Administrator`/`password`), and Inventory Service `http://localhost:8081`. Run each project with the `dev` profile. Production/Azure values are configured through the environment variables used by each service's `application.yaml`.

## API

```http
POST http://localhost:8080/orders
Content-Type: application/json

{"orderId":"O1001","productId":"P1001","quantity":5}
```

The order response is the inventory reduction response (`200`): `orderId`, `productId`, `quantity`, `previousCount`, `currentCount`, and `alreadyProcessed`. Inventory can be called directly at `POST http://localhost:8081/inventory/reduce` with the same body. Invalid quantity, missing product, insufficient stock, and idempotency conflicts return `400`; malformed/unmapped URLs return framework `404`; unexpected failures return `500`.

## Manual seed and SQL checks

No inventory-increase API or automatic seed is included. After Flyway has run, insert development stock manually:

```sql
INSERT INTO inventory(product_id, product_name, product_type, available_count)
VALUES ('P1001', 'Demo product', 'STANDARD', 25)
ON CONFLICT (product_id) DO UPDATE SET available_count = EXCLUDED.available_count,
  updated_at = CURRENT_TIMESTAMP;

SELECT * FROM inventory;
SELECT event_id, product_id, topic, status, retry_count, lease_until, published_at
FROM outbox_events ORDER BY created_at;
```

For local Couchbase setup, open the Couchbase UI at `http://localhost:8091`, create the `inventory` bucket if it was not provisioned automatically, then create the `Inventory` scope and `InventoryList` collection. The Inventory Query read model uses `inventory.Inventory.InventoryList`; it does not use Couchbase's `_default` collection. Execute the following after the bucket exists:

```sql
CREATE SCOPE `inventory`.Inventory;
CREATE COLLECTION `inventory`.Inventory.InventoryList;
CREATE PRIMARY INDEX ON `inventory`.`Inventory`.`InventoryList`;
```

For production, replace the primary index with a targeted index appropriate for the catalogue query. To reset local state, stop Compose and remove its volumes (`docker compose down -v`), then restart the services.
