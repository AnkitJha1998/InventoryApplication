# API Specification

## 1. Authentication

Protected APIs require:

`Authorization: Bearer <JWT>`

The JWT is issued by the User Service.

Each protected service independently validates the JWT and authorizes the request based on the user's role and the API being accessed.

When a protected service calls another protected service, the same user JWT is forwarded.

---

## 2. API Security

| API | Authentication | Authorization |
|---|---|---|
| `POST /users` | None | None |
| `POST /auth/login` | None | None |
| `POST /orders` | JWT | CUSTOMER |
| `POST /inventory/reduce` | JWT | Authenticated request |
| Inventory increase API | JWT | STORE_MANAGER |

The Inventory increase API is defined when the inventory management flow is implemented.

---

## 3. User Service APIs

### Register User

`POST /users`

Authentication: None

Request:

    {
      "username": "customer1",
      "password": "password",
      "role": "CUSTOMER"
    }

Success:

HTTP `201 Created`

    {
      "userId": "U1001",
      "username": "customer1",
      "role": "CUSTOMER"
    }

Failure:

- `400 Bad Request` — invalid request.
- `409 Conflict` — username already exists.

Passwords must never be returned in API responses.

---

### Login

`POST /auth/login`

Authentication: None

Request:

    {
      "username": "customer1",
      "password": "password"
    }

Success:

HTTP `200 OK`

    {
      "accessToken": "<JWT>",
      "tokenType": "Bearer",
      "expiresIn": 3600
    }

Failure:

- `401 Unauthorized` — invalid credentials.

---

## 4. Order Service APIs

### Create Order

`POST /orders`

Authentication: JWT

Authorization: `CUSTOMER`

Request:

    {
      "orderId": "O1001",
      "productId": "P1001",
      "quantity": 5
    }

The Order Service validates the customer's JWT and authorizes the request.

The Order Service then calls the Inventory Service to reduce inventory.

The same JWT is forwarded to the Inventory Service.

Success:

HTTP `200 OK`

    {
      "orderId": "O1001",
      "productId": "P1001",
      "quantity": 5,
      "status": "SUCCESS"
    }

Failure:

- `400 Bad Request` — invalid request.
- `401 Unauthorized` — missing or invalid JWT.
- `403 Forbidden` — authenticated user is not a Customer.
- `409 Conflict` — inventory reduction failed.

---

## 5. Inventory Service APIs

### Reduce Inventory

`POST /inventory/reduce`

Authentication: JWT

Authorization: Authenticated request

Request:

    {
      "orderId": "O1001",
      "productId": "P1001",
      "quantity": 5
    }

The Inventory Service validates the JWT before processing the request.

Success:

HTTP `200 OK`

    {
      "orderId": "O1001",
      "productId": "P1001",
      "quantity": 5,
      "remainingStock": 15,
      "status": "SUCCESS"
    }

Failure:

- `400 Bad Request` — invalid quantity or request.
- `401 Unauthorized` — missing or invalid JWT.
- `409 Conflict` — insufficient inventory or idempotency conflict.

If the same `orderId + productId + quantity` is received again, the previously recorded result should be returned without reducing inventory again.

If the same `orderId + productId` is received with a different quantity, the request must be rejected.

---

## 6. JWT Requirements

The JWT must contain:

- User identity.
- User role.
- Issued-at time.
- Expiry time.

The JWT must be digitally signed.

Protected services must validate:

- Token signature.
- Token expiry.
- Required claims.

The signing secret or signing key must be supplied through configuration and must not be hardcoded in source code.