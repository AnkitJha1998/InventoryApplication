\## Back-in-Stock Notification Requests



\- Expose `POST /notifications/back-in-stock`.

\- Allow only authenticated users with the CUSTOMER role.

\- Accept `productId` in the request.

\- Derive `customerId` from the validated JWT.

\- Persist notification requests in Notification Service PostgreSQL.

\- Prevent duplicate active requests for the same customer and product.

\- Do not modify inventory when creating a notification request.

\- Process NotificationTrigger events and deliver notifications as defined in Flow 5.

