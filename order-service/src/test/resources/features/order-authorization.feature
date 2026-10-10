Feature: Order authorization
  The Order Service accepts orders only from authenticated customers.

  Scenario: A CUSTOMER can create an order
    Given a valid CUSTOMER JWT
    When the customer creates an order
    Then the order response status should be 200

  Scenario: A STORE_MANAGER cannot create an order
    Given a valid STORE_MANAGER JWT
    When the customer creates an order
    Then the order response status should be 403

  Scenario: An order without a JWT is rejected
    When the customer creates an order
    Then the order response status should be 401

  Scenario: An order with an invalid JWT is rejected
    Given an invalid JWT
    When the customer creates an order
    Then the order response status should be 401

  Scenario: Order Service forwards the customer JWT to its order service
    Given a valid CUSTOMER JWT
    When the customer creates an order
    Then the order response status should be 200
    And the Order Service should receive the same Authorization header
