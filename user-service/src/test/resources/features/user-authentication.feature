Feature: User registration and authentication
  The User Service manages users and issues JWT access tokens.

  Scenario: A CUSTOMER can register
    When I register a user with username "customer-one", password "customer-password", and role "CUSTOMER"
    Then the registration response status should be 201
    And the registration response should contain username "customer-one" and role "CUSTOMER"
    And the registration response should contain a generated user id

  Scenario: A STORE_MANAGER can register
    When I register a user with username "manager-one", password "manager-password", and role "STORE_MANAGER"
    Then the registration response status should be 201
    And the registration response should contain username "manager-one" and role "STORE_MANAGER"

  Scenario: A registered CUSTOMER can log in
    Given a registered user with username "customer-two", password "customer-password", and role "CUSTOMER"
    When I log in with username "customer-two" and password "customer-password"
    Then the login response status should be 200
    And the login response should contain a Bearer access token
    And the access token should contain role "CUSTOMER"
    And the access token should contain userId, issued-at, and expiration claims

  Scenario: A registered STORE_MANAGER can log in
    Given a registered user with username "manager-two", password "manager-password", and role "STORE_MANAGER"
    When I log in with username "manager-two" and password "manager-password"
    Then the login response status should be 200
    And the login response should contain a Bearer access token
    And the access token should contain role "STORE_MANAGER"

  Scenario: Password is not returned after registration
    When I register a user with username "no-password", password "secret-password", and role "CUSTOMER"
    Then the registration response status should be 201
    And the registration response should not contain a password

  Scenario: Invalid credentials are rejected
    Given a registered user with username "invalid-login", password "correct-password", and role "CUSTOMER"
    When I log in with username "invalid-login" and password "wrong-password"
    Then the login response status should be 401

  Scenario: Duplicate username registration is rejected
    Given a registered user with username "duplicate-user", password "password", and role "CUSTOMER"
    When I register a user with username "duplicate-user", password "another-password", and role "CUSTOMER"
    Then the registration response status should be 409

  Scenario: Invalid role registration is rejected
    When I register a user with username "invalid-role", password "password", and role "ADMIN"
    Then the registration response status should be 400
