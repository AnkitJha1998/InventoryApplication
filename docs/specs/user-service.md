# User Service Specification

## Responsibility

The User Service manages user registration and authentication.

It supports Customer and Store Manager roles and issues JWT tokens after successful authentication.

## APIs

### Register User

`POST /users`

Request:

    {
      "username": "customer1",
      "password": "password",
      "role": "CUSTOMER"
    }

The service creates the user if the username is not already registered.

Passwords must be stored as secure password hashes.

### Login

`POST /auth/login`

Request:

    {
      "username": "customer1",
      "password": "password"
    }

The service validates the credentials and returns a JWT token.

## JWT

The JWT contains:

- User identity.
- User role.
- Token expiry.

The token must be signed.

Protected services must validate the JWT signature and expiry independently.

The JWT is supplied to protected APIs using:

`Authorization: Bearer <JWT>`

## Data

The User Service uses PostgreSQL for:

- User accounts.
- Password hashes.
- User roles.

Use Spring Data JPA with Hibernate for PostgreSQL persistence.

## Rules

- Username must be unique.
- Passwords must never be stored in plain text.
- Passwords must be hashed using a secure password-hashing algorithm.
- Only `CUSTOMER` and `STORE_MANAGER` roles are supported.
- Invalid credentials must be rejected.
- JWT tokens must have an expiry.
- JWT signing configuration must not be hardcoded in source code.
- Protected services must validate JWT tokens independently.
- The User Service must not proxy authenticated business requests to other services.

## Constraints

- The service must not access another service's database.
- Use Spring Security for authentication and JWT security.
- Use Spring Data JPA repositories rather than JDBC for PostgreSQL persistence.