Feature: Inventory creation persistence
  Inventory creation commits the product and its InventoryUpdate Outbox event together.

  Scenario: Successful creation persists the initial inventory and one Outbox event
    When the service creates product "Persistence Laptop" of type "ELECTRONICS" with quantity 5
    Then the created inventory row should exist
    And the stored quantity should be 5
    And the stored version should be 1
    And exactly one InventoryUpdate Outbox event should exist for the product
    And the Outbox event should be pending with previous count 0 and current count 5

  Scenario: Invalid creation creates no inventory or Outbox records
    When the service attempts to create product "Invalid Laptop" of type "ELECTRONICS" with quantity 0
    Then the creation should fail
    And no inventory row should exist for the attempted product
    And no Outbox event should exist for the attempted product
