# Local Application Test Curls

Run these commands from PowerShell after PostgreSQL, Couchbase, Kafka, and all four services are running with the `dev` profile.

## Service ports

```text
User Service:         http://localhost:8082
Inventory Service:    http://localhost:8081
Order Service:        http://localhost:8080
Notification Service: http://localhost:8083
```

## Start services

Kafka:

```powershell
& "C:\Kafka\kafka_2.13-4.3.1\bin\windows\kafka-server-start.bat" "C:\Kafka\kafka_2.13-4.3.1\config\server.properties"
```

User Service:

```powershell
cd user-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Inventory Service:

```powershell
cd inventory-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Order Service:

```powershell
cd order-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Notification Service:

```powershell
cd notification-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Kafka topics

Create the exact application topics once:

```powershell
& "C:\Kafka\kafka_2.13-4.3.1\bin\windows\kafka-topics.bat" --bootstrap-server localhost:9092 --create --if-not-exists --topic InventoryUpdate --partitions 1 --replication-factor 1
```

```powershell
& "C:\Kafka\kafka_2.13-4.3.1\bin\windows\kafka-topics.bat" --bootstrap-server localhost:9092 --create --if-not-exists --topic NotificationTrigger --partitions 1 --replication-factor 1
```

List topics:

```powershell
& "C:\Kafka\kafka_2.13-4.3.1\bin\windows\kafka-topics.bat" --bootstrap-server localhost:9092 --list
```

## 1. Register users

Register a Store Manager:

```powershell
$manager = curl.exe -s -X POST http://localhost:8082/users -H "Content-Type: application/json" -d '{"username":"manager1","password":"password","role":"STORE_MANAGER"}' | ConvertFrom-Json
$manager
```

Register a Customer:

```powershell
$customer = curl.exe -s -X POST http://localhost:8082/users -H "Content-Type: application/json" -d '{"username":"customer1","password":"password","role":"CUSTOMER"}' | ConvertFrom-Json
$customer
```

## 2. Login

```powershell
$managerLogin = curl.exe -s -X POST http://localhost:8082/auth/login -H "Content-Type: application/json" -d '{"username":"manager1","password":"password"}' | ConvertFrom-Json
$managerToken = $managerLogin.accessToken
```

```powershell
$customerLogin = curl.exe -s -X POST http://localhost:8082/auth/login -H "Content-Type: application/json" -d '{"username":"customer1","password":"password"}' | ConvertFrom-Json
$customerToken = $customerLogin.accessToken
```

## 3. Create inventory as Store Manager

```powershell
$managerHeaders = @{
    Authorization = "Bearer $managerToken"
}

curl.exe -i -X POST http://localhost:8081/inventory/create -H "Authorization: Bearer $managerToken" -H "Content-Type: application/json" -d '{"productId":"P1001","productName":"Demo Laptop","productType":"ELECTRONICS","quantity":5}'
```

Expected:

```text
200 OK
version: 1
```

## 4. Query inventory

Single product:

```powershell
curl.exe -i -X GET http://localhost:8081/inventory/P1001 -H "Authorization: Bearer $customerToken"
```

Catalogue:

```powershell
curl.exe -i -X GET "http://localhost:8081/inventory?page=0&size=20" -H "Authorization: Bearer $customerToken"
```

## 5. Reduce inventory through Order Service

Reduce the product from 5 to 0:

```powershell
curl.exe -i -X POST http://localhost:8080/orders -H "Authorization: Bearer $customerToken" -H "Content-Type: application/json" -d '{"orderId":"O1001","productId":"P1001","quantity":5}'
```

Expected:

```text
200 OK
currentCount: 0
```

## 6. Create back-in-stock request

```powershell
curl.exe -i -X POST http://localhost:8083/notifications/back-in-stock -H "Authorization: Bearer $customerToken" -H "Content-Type: application/json" -d '{"productId":"P1001"}'
```

Expected:

```text
201 Created
status: ACTIVE
```

Repeat the request to verify duplicate protection:

```powershell
curl.exe -i -X POST http://localhost:8083/notifications/back-in-stock -H "Authorization: Bearer $customerToken" -H "Content-Type: application/json" -d '{"productId":"P1001"}'
```

Expected:

```text
400 Bad Request
```

## 7. Increase inventory and trigger notification

Increase stock from zero to positive:

```powershell
curl.exe -i -X POST http://localhost:8081/inventory/increase -H "Authorization: Bearer $managerToken" -H "Content-Type: application/json" -d '{"productId":"P1001","quantity":5}'
```

Expected:

```text
200 OK
```

This should publish:

```text
InventoryUpdate
NotificationTrigger
```

The Notification Service should create a notification with:

```text
Demo Laptop is now available
status = CREATED
```

The back-in-stock request should become:

```text
COMPLETED
```

## 8. Read notification count

```powershell
curl.exe -i -X GET http://localhost:8083/notifications/count -H "Authorization: Bearer $customerToken"
```

Expected shape:

```json
{
  "unreadCount": 1
}
```

## 9. List notifications

```powershell
curl.exe -i -X GET "http://localhost:8083/notifications?page=0&size=20" -H "Authorization: Bearer $customerToken"
```

Expected notification fields:

```text
notificationId
productId
message
read: false
createdAt
```

## 10. Mark notifications as read

Replace `notification-id-from-response` with the returned notification ID:

```powershell
curl.exe -i -X PATCH http://localhost:8083/notifications/read -H "Authorization: Bearer $customerToken" -H "Content-Type: application/json" -d '{"notificationIds":["notification-id-from-response"]}'
```

Expected:

```json
{
  "updatedCount": 1
}
```

Verify unread count is now zero:

```powershell
curl.exe -i -X GET http://localhost:8083/notifications/count -H "Authorization: Bearer $customerToken"
```

## 11. Authorization checks

Customer cannot create inventory:

```powershell
curl.exe -i -X POST http://localhost:8081/inventory/create -H "Authorization: Bearer $customerToken" -H "Content-Type: application/json" -d '{"productId":"P2001","productName":"Unauthorized Product","productType":"STANDARD","quantity":5}'
```

Expected:

```text
403 Forbidden
```

Store Manager cannot create a notification request:

```powershell
curl.exe -i -X POST http://localhost:8083/notifications/back-in-stock -H "Authorization: Bearer $managerToken" -H "Content-Type: application/json" -d '{"productId":"P1001"}'
```

Expected:

```text
403 Forbidden
```

Missing JWT:

```powershell
curl.exe -i -X GET http://localhost:8083/notifications/count
```

Expected:

```text
401 Unauthorized
```

## 12. Idle NotificationTrigger behavior

When `NotificationTrigger` arrives for a product with no ACTIVE back-in-stock requests:

```text
No notification is created.
No request is updated.
The event is acknowledged.

# Postman-Importable cURL Commands

Import each command separately in Postman using **Import → Raw text**. Replace the token placeholders with tokens returned by the login endpoint.

## Register Store Manager

```bash
curl --location --request POST 'http://localhost:8082/users' --header 'Content-Type: application/json' --data '{"username":"manager1","password":"password","role":"STORE_MANAGER"}'
```

## Register Customer

```bash
curl --location --request POST 'http://localhost:8082/users' --header 'Content-Type: application/json' --data '{"username":"customer1","password":"password","role":"CUSTOMER"}'
```

## Login Store Manager

```bash
curl --location --request POST 'http://localhost:8082/auth/login' --header 'Content-Type: application/json' --data '{"username":"manager1","password":"password"}'
```

## Login Customer

```bash
curl --location --request POST 'http://localhost:8082/auth/login' --header 'Content-Type: application/json' --data '{"username":"customer1","password":"password"}'
```

## Create Inventory

```bash
curl --location --request POST 'http://localhost:8081/inventory/create' --header 'Authorization: Bearer <MANAGER_TOKEN>' --header 'Content-Type: application/json' --data '{"productId":"P1001","productName":"Demo Laptop","productType":"ELECTRONICS","quantity":5}'
```

## Get One Inventory Product

```bash
curl --location --request GET 'http://localhost:8081/inventory/P1001' --header 'Authorization: Bearer <CUSTOMER_TOKEN>'
```

## Get Inventory Catalogue

```bash
curl --location --request GET 'http://localhost:8081/inventory?page=0&size=20' --header 'Authorization: Bearer <CUSTOMER_TOKEN>'
```

## Create Order and Reduce Inventory

```bash
curl --location --request POST 'http://localhost:8080/orders' --header 'Authorization: Bearer <CUSTOMER_TOKEN>' --header 'Content-Type: application/json' --data '{"orderId":"O1001","productId":"P1001","quantity":5}'
```

## Create Back-in-Stock Request

```bash
curl --location --request POST 'http://localhost:8083/notifications/back-in-stock' --header 'Authorization: Bearer <CUSTOMER_TOKEN>' --header 'Content-Type: application/json' --data '{"productId":"P1001"}'
```

## Increase Inventory

```bash
curl --location --request POST 'http://localhost:8081/inventory/increase' --header 'Authorization: Bearer <MANAGER_TOKEN>' --header 'Content-Type: application/json' --data '{"productId":"P1001","quantity":5}'
```

## Get Unread Notification Count

```bash
curl --location --request GET 'http://localhost:8083/notifications/count' --header 'Authorization: Bearer <CUSTOMER_TOKEN>'
```

## List Notifications

```bash
curl --location --request GET 'http://localhost:8083/notifications?page=0&size=20' --header 'Authorization: Bearer <CUSTOMER_TOKEN>'
```

## Mark Notifications as Read

Replace the notification ID with the value returned by the list endpoint.

```bash
curl --location --request PATCH 'http://localhost:8083/notifications/read' --header 'Authorization: Bearer <CUSTOMER_TOKEN>' --header 'Content-Type: application/json' --data '{"notificationIds":["<NOTIFICATION_ID>"]}'
```

## Customer Authorization Check

```bash
curl --location --request POST 'http://localhost:8081/inventory/create' --header 'Authorization: Bearer <CUSTOMER_TOKEN>' --header 'Content-Type: application/json' --data '{"productId":"P2001","productName":"Unauthorized Product","productType":"STANDARD","quantity":5}'
```

Expected response:

```text
403 Forbidden
```

## Missing JWT Check

```bash
curl --location --request GET 'http://localhost:8083/notifications/count'
```

Expected response:

```text
401 Unauthorized
```
```
