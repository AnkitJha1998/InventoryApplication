# Flow 02 - User Registration and Authentication

## Purpose

Allow users to register as Customer or Store Manager and authenticate using JWT.

## Registration

    Consumer
        |
        | POST /users
        v
    User Service
        |
        v
    PostgreSQL
        |
        +--> User created
        +--> Password stored as hash

## Login

    Consumer
        |
        | POST /auth/login
        v
    User Service
        |
        v
    PostgreSQL
        |
        +--> Validate credentials
        |
        v
    JWT Token
        |
        v
    Consumer

## Authenticated Request

    Consumer
        |
        | Authorization: Bearer <JWT>
        v
    Protected Service
        |
        +--> Validate JWT
        +--> Authorize request
        |
        v
    Business Operation

## Service-to-Service Request

The existing user JWT is forwarded when one protected service calls another protected service.

    Consumer
        |
        | JWT
        v
    Order Service
        |
        | Same JWT
        v
    Inventory Service

The receiving service validates the JWT before processing the request.

## Key Rules

- A user can register as Customer or Store Manager.
- Passwords must never be stored in plain text.
- User Service authenticates credentials and issues JWT tokens.
- JWT tokens contain the user's identity and role.
- Protected services independently validate JWT tokens.
- Protected services authorize requests based on the JWT and the API being accessed.
- The user's JWT is forwarded for authenticated service-to-service requests.
- Invalid or expired JWT tokens must be rejected.
- Users must not be able to access operations outside their assigned role.

## Out of Scope

- Refresh token management.
- OAuth2 / OpenID Connect.
- Social login.
- Password reset.
- Multi-factor authentication.
- Session management.