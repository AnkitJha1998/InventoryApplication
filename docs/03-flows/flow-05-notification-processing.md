# Flow 05 - In-App Notification Processing

## Purpose

Create in-app notifications for customers who requested back-in-stock alerts and provide APIs to retrieve and bulk-mark their notifications as read.

## Flow A - Process Back-in-Stock Event

Kafka topic `NotificationTrigger` delivers the InventoryUpdate-compatible payload. The Notification Service validates the payload, checks the processed-event table, and finds ACTIVE requests for the product. In one checked-exception transactional operation it creates one notification per request with status `CREATED` and message `<productName> is now available`, marks requests `COMPLETED`, and records the event ID. Kafka acknowledgment occurs only after commit. Malformed payloads, duplicate event IDs, and events without active requests are acknowledged without creating records.

## Flow B - Retrieve Notifications

A CUSTOMER calls `GET /notifications/count` or `GET /notifications?page=0&size=20`. The service validates JWT ownership, counts only `CREATED` records, or returns customer-scoped notifications ordered by `createdAt DESC, notificationId ASC` with pagination metadata.

## Flow C - Bulk Mark Notifications as Read

A CUSTOMER calls `PATCH /notifications/read` with a non-empty `notificationIds` list. The service updates only matching records owned by the authenticated customer and currently in `CREATED` status. Already-read and other-customer IDs are ignored, and the response reports `updatedCount`. There is no single-notification read endpoint.

## Rules

- Consume events from Kafka topic `NotificationTrigger` with group `notification-consumer`.
- Notification statuses are exactly `CREATED` (unread/new) and `NOTIFIED` (read).
- Back-in-stock request statuses are exactly `ACTIVE` and `COMPLETED`.
- Store notifications in Notification Service PostgreSQL.
- Keep notification creation, request completion, and event processing consistent.
- Prevent duplicate notifications when Kafka events are redelivered.
- Do not send email, SMS, or push notifications.
- Customers can retrieve and update only their own notifications.
