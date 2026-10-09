\# Flow 05 - In-App Notification Processing



\## Purpose



Create in-app notifications for customers who requested back-in-stock alerts and provide APIs to retrieve and manage their notifications.



\## Flow A - Process Back-in-Stock Event



Kafka: NotificationTrigger

&#x20;   |

&#x20;   v

Notification Service

&#x20;   |

&#x20;   | Find ACTIVE requests matching productId

&#x20;   | Prevent duplicate event processing

&#x20;   v

Notification PostgreSQL Transaction

&#x20;   |

&#x20;   | Create in-app notification for each matching customer

&#x20;   | Mark corresponding request as NOTIFIED

&#x20;   | Record event processing

&#x20;   v

Processing Complete



\## Flow B - Retrieve Notifications



Customer UI

&#x20;   |

&#x20;   | GET /notifications/count

&#x20;   | GET /notifications?page=0\&size=20

&#x20;   v

Notification Service

&#x20;   |

&#x20;   | Validate JWT

&#x20;   | Identify customer from JWT

&#x20;   v

Notification PostgreSQL

&#x20;   |

&#x20;   | Retrieve unread count or paginated notifications

&#x20;   v

Return Notification Data



\## Flow C - Mark Notifications as Read



Customer UI

&#x20;   |

&#x20;   | PATCH /notifications/{notificationId}/read

&#x20;   | PATCH /notifications/read

&#x20;   v

Notification Service

&#x20;   |

&#x20;   | Validate JWT

&#x20;   | Identify customer from JWT

&#x20;   | Update only the customer's notifications

&#x20;   v

Notification PostgreSQL

&#x20;   |

&#x20;   v

Return Update Result



\## Rules



\- Consume events from Kafka topic NotificationTrigger.

\- Create notifications only for matching ACTIVE notification requests.

\- Store notifications in Notification Service PostgreSQL.

\- Create notifications, update request status, and record event processing consistently.

\- Prevent duplicate notifications when Kafka events are redelivered.

\- Do not send email, SMS, or push notifications.

\- Customers can retrieve and update only their own notifications.

\- Support unread count and zero-based paginated retrieval.

\- Already-read notifications remain read when read operations are repeated.

\- Notification request status is independent of notification read status.

