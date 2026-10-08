\# Functional Requirements



\## User Management



\### FR-001 User Registration

A user should be able to register as a Customer or Store Manager by providing the required user details.



\### FR-002 User Authentication and Authorization

The system should authenticate users and allow them to access only the operations permitted for their role using a valid authentication token.



\## Inventory Management



\### FR-003 Inventory Management

A Store Manager should be able to increase the available stock of products in the store.



\### FR-004 Inventory Reduction

The system should reduce product inventory when requested by the Order Service and should reject the operation when sufficient stock is not available.



\### FR-005 Inventory Operation Idempotency

The system should ensure that retrying the same inventory reduction request does not reduce the inventory more than once.



\## Inventory Query



\### FR-006 Inventory Query

A Customer should be able to retrieve the inventory and availability information for a product and browse the product catalogue using pagination.



\## Notifications



\### FR-007 Back-in-Stock Notification Request

A Customer should be able to request a notification when an unavailable product becomes available, and the system should prevent multiple active requests for the same Customer and product.



\### FR-008 Back-in-Stock Notification

When a product changes from unavailable to available, the system should generate a notification for Customers with an active request and complete those requests.



\### FR-009 Notification Management

A Customer should be able to retrieve their notifications and mark notifications as read.

