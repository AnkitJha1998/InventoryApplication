Feature: Order inventory reduction
  A valid order reduces inventory exactly once and creates an InventoryUpdate event.

  Scenario: A valid order reduces inventory
    Given inventory product "order-product" exists with quantity 5
    When order "order-1" reduces product "order-product" by 2
    Then the reduction current quantity should be 3
    And one inventory operation should exist for order "order-1"
    And one InventoryUpdate Outbox event should exist for product "order-product"

  Scenario: Repeating the same order is idempotent
    Given inventory product "idempotent-product" exists with quantity 5
    When order "order-2" reduces product "idempotent-product" by 2
    And order "order-2" reduces product "idempotent-product" by 2 again
    Then the second reduction should be marked already processed
    And the product quantity should remain 3
    And one inventory operation should exist for order "order-2"

  Scenario: Reusing an order with a different quantity is rejected
    Given inventory product "conflict-product" exists with quantity 5
    When order "order-3" reduces product "conflict-product" by 2
    And order "order-3" reduces product "conflict-product" by 3 again
    Then the reduction should fail with an idempotency conflict

  Scenario: Insufficient inventory is rejected
    Given inventory product "limited-product" exists with quantity 2
    When order "order-4" reduces product "limited-product" by 3
    Then the reduction should fail with insufficient stock
    And the product quantity should remain 2
