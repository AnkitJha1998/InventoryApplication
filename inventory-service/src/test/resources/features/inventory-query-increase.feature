Feature: Inventory query and increase
  Customers can query inventory and store managers can increase stock.

  Scenario: A CUSTOMER can query one product
    Given a query CUSTOMER JWT
    When the customer queries product "product-1"
    Then the inventory query response status should be 200
    And the inventory response should contain product "product-1" with quantity 5 and version 1

  Scenario: A STORE_MANAGER can query one product
    Given a query STORE_MANAGER JWT
    When the customer queries product "product-1"
    Then the inventory query response status should be 200

  Scenario: A missing product returns 404
    Given a query CUSTOMER JWT
    When the customer queries product "missing-product"
    Then the inventory query response status should be 404

  Scenario: Invalid pagination returns 400
    Given a query CUSTOMER JWT
    When the customer browses inventory with page 0 and size 101
    Then the inventory query response status should be 400

  Scenario: A CUSTOMER cannot increase inventory
    Given a query CUSTOMER JWT
    When the manager increases product "product-1" by 2
    Then the inventory query response status should be 403

  Scenario: A STORE_MANAGER can increase inventory
    Given a query STORE_MANAGER JWT
    When the manager increases product "product-1" by 2
    Then the inventory query response status should be 200
    And the increase response should contain quantity 7 and version 2

  Scenario: A non-positive increase is rejected
    Given a query STORE_MANAGER JWT
    When the manager increases product "product-1" by 0
    Then the inventory query response status should be 400
