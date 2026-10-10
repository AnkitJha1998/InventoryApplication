# Flow 5 - Implementation Tasks

## Event Processing

- [x] Configure the Notification Service consumer for Kafka topic `NotificationTrigger` and group `notification-consumer`.
- [x] Find ACTIVE back-in-stock requests matching productId.
- [x] Create one `CREATED` in-app notification per matching request with the exact product message.
- [x] Mark corresponding requests `COMPLETED`.
- [x] Record processed events and prevent duplicate notifications.
- [x] Acknowledge malformed, duplicate, and idle events safely; retry transactional failures.

## Notification APIs

- [x] Implement GET `/notifications/count`.
- [x] Implement GET `/notifications` with page 0 default, size 1..100, and stable ordering.
- [x] Implement PATCH `/notifications/read` for bulk customer-scoped updates.
- [x] Enforce JWT authentication and CUSTOMER authorization.
- [x] Ensure customers can access only their own notifications.
- [x] Verify unreadCount and updatedCount behavior.
- [x] Verify repeated bulk read requests are safe.
- [x] Do not implement a single-notification read endpoint.

## Verification

- [x] Test matching and non-matching notification requests.
- [x] Test duplicate Kafka event delivery and malformed payload acknowledgment.
- [x] Test notification creation, exact message, statuses, and request completion.
- [x] Test unread count, ownership, pagination, and bulk read behavior.
- [x] Test downstream query failures return structured HTTP 500.
- [x] Verify no external email, SMS, or push delivery is implemented.
