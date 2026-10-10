# Manual Flow 4 Testing

## Purpose

This document describes how to manually verify Flow 4 after PostgreSQL, Couchbase, Kafka, and all services are running.

Flow 4 verifies:

```text
Customer creates back-in-stock request
Inventory moves from zero to positive
Inventory publishes NotificationTrigger
Notification Service creates a notification entry
```

## Prerequisites

### PostgreSQL

PostgreSQL must be available at:

```text
localhost:5432
```

The current development profiles use database:

```text
inventory
```

### Couchbase

Couchbase must be available at:

```text
http://localhost:8091
```

Required resources:

```text
Bucket:     inventory
Scope:      Inventory
Collection: InventoryList
```

Create the development index if it does not already exist:

```sql
CREATE PRIMARY INDEX ON `inventory`.`Inventory`.`InventoryList`;
```

### Kafka

Kafka is installed at:

```text
C:\Kafka\kafka_2.13-4.3.1
```

Start it in a separate PowerShell terminal:

```powershell
& "C:\Kafka\kafka_2.13-4.3.1\bin\windows\kafka-server-start.bat" `
  "C:\Kafka\kafka_2.13-4.3.1\config\server.properties"
```

Verify that Kafka is listening on:

```text
localhost:9092
```

## Start the services

Start each service in a separate terminal from the repository root.

### Inventory Service

```powershell
cd inventory-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Port:

```text
8081
```

### User Service

```powershell
cd user-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Port:

```text
8082
```

### Order Service

```powershell
cd order-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Port:

```text
8080
```

### Notification Service

```powershell
cd notification-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Port:

```text
8083
```

## 1. Register a Store Manager

```powershell
$managerRegistration = Invoke-RestMethod `
  -Uri http://localhost:8082/users `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"username":"manager1","password":"password","role":"STORE_MANAGER"}'

$managerRegistration
```

Expected result:

```text
201 Created
```

## 2. Log in as Store Manager

```powershell
$managerLogin = Invoke-RestMethod `
  -Uri http://localhost:8082/auth/login `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"username":"manager1","password":"password"}'

$managerToken = $managerLogin.accessToken
$managerToken
```

## 3. Register a Customer

```powershell
$customerRegistration = Invoke-RestMethod `
  -Uri http://localhost:8082/users `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"username":"customer1","password":"password","role":"CUSTOMER"}'

$customerRegistration
```

## 4. Log in as Customer

```powershell
$customerLogin = Invoke-RestMethod `
  -Uri http://localhost:8082/auth/login `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"username":"customer1","password":"password"}'

$customerToken = $customerLogin.accessToken
$customerToken
```

## 5. Create inventory

Create a product with initial stock:

```powershell
$headers = @{ Authorization = "Bearer $managerToken" }

Invoke-RestMethod `
  -Uri http://localhost:8081/inventory/create `
  -Method Post `
  -Headers $headers `
  -ContentType "application/json" `
  -Body '{"productId":"P1001","productName":"Demo Laptop","productType":"ELECTRONICS","quantity":5}'
```

Expected result:

```text
200 OK
version = 1
```

The Inventory Query Consumer should eventually create the Couchbase document.

## 6. Reduce inventory to zero through Order Service

The product has quantity `5`, so create an order for quantity `5`:

```powershell
$orderHeaders = @{ Authorization = "Bearer $customerToken" }

Invoke-RestMethod `
  -Uri http://localhost:8080/orders `
  -Method Post `
  -Headers $orderHeaders `
  -ContentType "application/json" `
  -Body '{"orderId":"O1001","productId":"P1001","quantity":5}'
```

Expected result:

```text
200 OK
currentCount = 0
```

## 7. Create a back-in-stock request

The product is now unavailable:

```powershell
$notificationHeaders = @{ Authorization = "Bearer $customerToken" }

Invoke-RestMethod `
  -Uri http://localhost:8083/notifications/back-in-stock `
  -Method Post `
  -Headers $notificationHeaders `
  -ContentType "application/json" `
  -Body '{"productId":"P1001"}'
```

Expected result:

```text
201 Created
status = ACTIVE
```

## 8. Verify duplicate request rejection

Repeat the same request:

```powershell
Invoke-RestMethod `
  -Uri http://localhost:8083/notifications/back-in-stock `
  -Method Post `
  -Headers $notificationHeaders `
  -ContentType "application/json" `
  -Body '{"productId":"P1001"}'
```

Expected result:

```text
400 Bad Request
```

## 9. Increase inventory from zero to positive

```powershell
Invoke-RestMethod `
  -Uri http://localhost:8081/inventory/increase `
  -Method Post `
  -Headers $headers `
  -ContentType "application/json" `
  -Body '{"productId":"P1001","quantity":5}'
```

Expected result:

```text
200 OK
```

Because the transition is:

```text
0 -> 5
```

Inventory Service should create:

```text
InventoryUpdate
NotificationTrigger
```

## 10. Verify the NotificationTrigger result

The Notification Service should consume the trigger and create a notification entry with:

```text
message = Demo Laptop is now available
status  = CREATED
```

The matching back-in-stock request should become:

```text
COMPLETED
```

The same NotificationTrigger must not create a duplicate notification when delivered again.

## 11. Verify idle trigger behavior

Create or publish a NotificationTrigger for a product that has no active back-in-stock requests.

Expected behavior:

```text
No notification entry is created.
No request is updated.
The event is acknowledged.
The consumer remains idle.
```

## 12. Verify role behavior

### CUSTOMER cannot create inventory

Call:

```powershell
Invoke-RestMethod `
  -Uri http://localhost:8081/inventory/create `
  -Method Post `
  -Headers $orderHeaders `
  -ContentType "application/json" `
  -Body '{"productId":"P2001","productName":"Other Product","productType":"STANDARD","quantity":5}'
```

Expected:

```text
403 Forbidden
```

### STORE_MANAGER cannot create a customer notification request

```powershell
Invoke-RestMethod `
  -Uri http://localhost:8083/notifications/back-in-stock `
  -Method Post `
  -Headers $headers `
  -ContentType "application/json" `
  -Body '{"productId":"P1001"}'
```

Expected:

```text
403 Forbidden
```

## Database verification

Use pgAdmin or `psql` to verify the Notification Service tables:

```sql
SELECT *
FROM back_in_stock_requests
ORDER BY created_at;

SELECT *
FROM notifications
ORDER BY created_at;

SELECT *
FROM processed_events
ORDER BY processed_at;
```

Expected notification message:

```text
Demo Laptop is now available
```

Expected request lifecycle:

```text
ACTIVE -> COMPLETED
```

## Known runtime dependency

This manual flow requires all of the following to be running:

```text
PostgreSQL  localhost:5432
Kafka       localhost:9092
Couchbase   localhost:8091 / 11210
User        localhost:8082
Inventory   localhost:8081
Order       localhost:8080
Notification localhost:8083
```
