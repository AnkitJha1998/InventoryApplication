Feature: NotificationTrigger processing
  NotificationTrigger events create in-app notifications for active requests.

  Scenario: A valid trigger is processed and acknowledged
    Given a trigger notification CUSTOMER context
    And the notification processor succeeds
    When the NotificationTrigger event is consumed
    Then the notification trigger should be acknowledged

  Scenario: A malformed trigger is acknowledged without processing
    Given a trigger notification CUSTOMER context
    When a malformed NotificationTrigger event is consumed
    Then the malformed trigger should be acknowledged

  Scenario: A processing failure does not acknowledge the trigger
    Given a trigger notification CUSTOMER context
    And the notification processor fails
    When the NotificationTrigger event is consumed
    Then the failed notification trigger should not be acknowledged
