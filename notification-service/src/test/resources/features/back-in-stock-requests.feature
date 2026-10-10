Feature: Back-in-stock requests
  Customers can request notification when an out-of-stock product becomes available.

  Scenario: A CUSTOMER can request an out-of-stock product
    Given a back-in-stock CUSTOMER JWT
    When the customer requests back-in-stock for product "product-1"
    Then the back-in-stock response status should be 201
    And the response should contain product "product-1" with status "ACTIVE"

  Scenario: A STORE_MANAGER cannot request back-in-stock
    Given a back-in-stock STORE_MANAGER JWT
    When the customer requests back-in-stock for product "product-1"
    Then the back-in-stock response status should be 403

  Scenario: A request without a JWT is rejected
    When the customer requests back-in-stock for product "product-1"
    Then the back-in-stock response status should be 401

  Scenario: A request with an invalid JWT is rejected
    Given an invalid back-in-stock JWT
    When the customer requests back-in-stock for product "product-1"
    Then the back-in-stock response status should be 401

  Scenario: A duplicate active request is rejected
    Given a back-in-stock CUSTOMER JWT
    And an existing active request for product "product-1"
    When the customer requests back-in-stock for product "product-1"
    Then the back-in-stock response status should be 400

  Scenario: A request for an in-stock product is rejected
    Given a back-in-stock CUSTOMER JWT
    And the product is already in stock
    When the customer requests back-in-stock for product "product-1"
    Then the back-in-stock response status should be 400

  Scenario: The Authorization header is forwarded to Inventory Service
    Given a back-in-stock CUSTOMER JWT
    When the customer requests back-in-stock for product "product-1"
    Then the back-in-stock response status should be 201
    And the Inventory Service should receive the same Authorization header
