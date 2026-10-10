Feature: Notification management
  Customers can view and mark their in-app notifications.

  Scenario: A CUSTOMER can retrieve unread count
    Given a notification CUSTOMER JWT
    When the customer retrieves the unread notification count
    Then the notification count response status should be 200
    And the unread count should be 2

  Scenario: A CUSTOMER can retrieve paginated notifications
    Given a notification CUSTOMER JWT
    When the customer retrieves notifications page 0 with size 20
    Then the notification list response status should be 200
    And the notification list should contain 1 notification

  Scenario: Invalid notification pagination returns 400
    Given a notification CUSTOMER JWT
    When the customer retrieves notifications page 0 with size 101
    Then the notification list response status should be 400

  Scenario: A CUSTOMER can bulk mark notifications as read
    Given a notification CUSTOMER JWT
    When the customer marks notification "00000000-0000-0000-0000-000000000001" as read
    Then the mark-read response status should be 200
    And the updated notification count should be 1

  Scenario: A STORE_MANAGER cannot access notification management
    Given a notification STORE_MANAGER JWT
    When the customer retrieves the unread notification count
    Then the notification count response status should be 403

  Scenario: Empty mark-read requests are rejected
    Given a notification CUSTOMER JWT
    When the customer submits an empty mark-read request
    Then the mark-read response status should be 400
