## Back-in-Stock Notification Requests

- Expose `POST /notifications/back-in-stock`.
- Allow only authenticated users with the CUSTOMER role.
- Accept `productId` in the request.
- Derive `customerId` from the validated JWT.
- Persist notification requests in Notification Service PostgreSQL.
- Prevent duplicate active requests for the same customer and product.
- Do not modify inventory when creating a notification request.
- Process NotificationTrigger events and deliver notifications as defined in Flow 5.

# Notification Service Specification

## Responsibilities

- Manage customer back-in-stock notification requests.
- Consume NotificationTrigger events.
- Create in-app notifications in PostgreSQL.
- Provide APIs to retrieve, count, and mark notifications as read.

## Back-in-Stock Notification Requests

- Expose POST /notifications/back-in-stock.
- Allow only authenticated users with the CUSTOMER role.
- Accept productId in the request.
- Derive customerId from the validated JWT.
- Persist requests in Notification Service PostgreSQL.
- Prevent duplicate ACTIVE requests for the same customer and product.
- Do not modify inventory when creating a notification request.

## Notification Processing

- Consume NotificationTrigger events from Kafka.
- Find ACTIVE notification requests matching the event's productId.
- Create an in-app notification for each matching customer.
- Mark corresponding back-in-stock requests as COMPLETED.
- Record processed events to prevent duplicate notifications.
- Handle duplicate event delivery safely.
- Do not send email, SMS, or push notifications.
- Do not access or modify Inventory Service database tables directly.

## Notification Data

Each notification must contain:

- notificationId
- customerId
- productId
- message
- read
- createdAt

New notifications must have read set to false.

Back-in-stock request status is ACTIVE or COMPLETED; notification status is CREATED (unread) or NOTIFIED (read).

Notification read status is independent of notification request status.

New notifications contain the exact message `<productName> is now available`.

## Notification APIs

### Get Unread Notification Count

- Endpoint: GET /notifications/count
- Authorization: CUSTOMER
- Return the authenticated customer's unread notification count.
- Response field: unreadCount.

### Get Paginated Notifications

- Endpoint: GET /notifications?page=0&size=20
- Authorization: CUSTOMER
- Return only notifications belonging to the authenticated customer.
- Use zero-based pagination.
- Default page: 0.
- Default size: 20.
- Maximum size: 100.
- Return content, page, size, totalElements, and totalPages.
- Include notificationId, productId, message, read, and createdAt in each item.

### Mark Multiple Notifications as Read

- Endpoint: PATCH /notifications/read
- Authorization: CUSTOMER
- Accept a list of notificationIds.
- Mark matching notifications belonging to the authenticated customer as read.
- Ignore notifications that are already read.
- Do not modify notifications belonging to other customers.
- Return updatedCount, representing the number changed from unread to read.
- Repeated requests must be safe.

## Security and Data Integrity

- Validate JWTs and derive customerId from the authenticated principal.
- Never accept customerId from the request body for ownership decisions.
- Enforce customer ownership in database queries and updates.
- Use transactional processing to keep notification creation, request status updates, and event-processing records consistent.
- Ensure Kafka redelivery does not create duplicate notifications.