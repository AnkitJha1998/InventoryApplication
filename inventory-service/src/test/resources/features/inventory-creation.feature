Feature: Inventory creation authorization and validation
  Inventory creation is restricted to store managers.

  Scenario: A STORE_MANAGER creates inventory
    Given a valid STORE_MANAGER JWT
    When the manager creates product "Demo Laptop" of type "ELECTRONICS" with quantity 5
    Then the inventory creation response status should be 200
    And the response should contain a generated product id
    And the response version should be 1

  Scenario: A CUSTOMER cannot create inventory
    Given a valid CUSTOMER JWT
    When the manager creates product "Demo Laptop" of type "ELECTRONICS" with quantity 5
    Then the inventory creation response status should be 403

  Scenario: Inventory creation without a JWT is rejected
    When the manager creates product "Demo Laptop" of type "ELECTRONICS" with quantity 5
    Then the inventory creation response status should be 401

  Scenario: Inventory creation with an invalid JWT is rejected
    Given an invalid JWT
    When the manager creates product "Demo Laptop" of type "ELECTRONICS" with quantity 5
    Then the inventory creation response status should be 401

  Scenario: A non-positive quantity is rejected
    Given a valid STORE_MANAGER JWT
    When the manager creates product "Demo Laptop" of type "ELECTRONICS" with quantity 0
    Then the inventory creation response status should be 400

  Scenario: A blank product name is rejected
    Given a valid STORE_MANAGER JWT
    When the manager creates product "" of type "ELECTRONICS" with quantity 5
    Then the inventory creation response status should be 400
