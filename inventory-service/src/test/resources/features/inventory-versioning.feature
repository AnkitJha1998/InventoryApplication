Feature: Versioned InventoryUpdate projection
  InventoryUpdate events carry a business version for CAS-protected projection updates.

  Scenario: A newer InventoryUpdate is handed to the CAS writer
    When an InventoryUpdate event for "versioned-product" with version 2 and quantity 8 is consumed
    Then the CAS writer should receive product "versioned-product" with version 2 and quantity 8
    And the InventoryUpdate event should be acknowledged

  Scenario: An equal-version InventoryUpdate is safely handled
    When an InventoryUpdate event for "versioned-product" with version 2 and quantity 8 is consumed
    Then the CAS writer should receive product "versioned-product" with version 2 and quantity 8
    And the InventoryUpdate event should be acknowledged

  Scenario: An older InventoryUpdate is safely handled
    When an InventoryUpdate event for "versioned-product" with version 1 and quantity 4 is consumed
    Then the CAS writer should receive product "versioned-product" with version 1 and quantity 4
    And the InventoryUpdate event should be acknowledged

  Scenario: A Couchbase write failure prevents acknowledgment
    Given the CAS writer is unavailable
    When an InventoryUpdate event for "versioned-product" with version 2 and quantity 8 is consumed
    Then the InventoryUpdate event should not be acknowledged
