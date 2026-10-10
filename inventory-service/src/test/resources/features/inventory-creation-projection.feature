Feature: Inventory creation event projection
  Inventory creation produces an InventoryUpdate event consumed by the query model.

  Scenario: A created inventory item produces an InventoryUpdate payload
    When the service creates product "Event Laptop" of type "ELECTRONICS" with quantity 5
    And the Outbox Worker publishes pending events
    Then the published event should use the InventoryUpdate topic
    And the published event should contain the created product id, version 1, previous count 0, and current count 5

  Scenario: An InventoryUpdate event is handed to the Couchbase writer
    When the service creates product "Projection Laptop" of type "ELECTRONICS" with quantity 5
    And the Outbox Worker publishes pending events
    And the InventoryUpdate consumer processes the published event
    Then the Couchbase writer should receive the created product with version 1 and quantity 5
    And the Kafka event should be acknowledged
