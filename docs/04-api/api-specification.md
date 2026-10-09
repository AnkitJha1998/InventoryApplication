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
- `400 Bad Request` — inventory reduction failed.

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
- `400 Bad Request` — insufficient inventory or idempotency conflict.

If the same `orderId + productId + quantity` is received again, the previously recorded result should be returned without reducing inventory again.

If the same `orderId + productId` is received with a different quantity, the request must be rejected.

---

## 6. JWT Requirements

The JWT must contain exactly these claims:

- `userId` — the server-generated user UUID.
- `role` — `CUSTOMER` or `STORE_MANAGER`.
- `iat` — issued-at time as a Unix timestamp.
- `exp` — expiry time as a Unix timestamp (3600 seconds after issuance).

Tokens use an HMAC-SHA256 signature. The signing secret is externalized (`JWT_SECRET` in production and the development profile secret locally). Protected endpoints reject missing, malformed, incorrectly signed, or expired tokens with `401 Unauthorized`; a valid token with an insufficient role receives `403 Forbidden`. The Order Service forwards the incoming `Authorization` header unchanged to the Inventory Service.

The JWT must be digitally signed.

Protected services must validate:

- Token signature.
- Token expiry.
- Required claims.

The signing secret or signing key must be supplied through configuration and must not be hardcoded in source code.


## 7. Inventory Query APIs

### Get Product Inventory

`GET /inventory/{productId}`

Authentication: JWT

Authorization: `CUSTOMER` or `STORE_MANAGER`

Returns the inventory query model for the requested product from Couchbase.

Success:

HTTP `200 OK`

    {
      "productId": "P1001",
      "productName": "Example Product",
      "productType": "STANDARD",
      "availableCount": 15,
      "availableQuantity": 15,
      "available": true,
      "updatedAt": "2025-01-01T00:00:00Z"
    }

Failure:

- `401 Unauthorized` — missing or invalid JWT.
- `403 Forbidden` — authenticated user does not have an allowed role.
- `404 Not Found` — product is not present in the query model.

### 8. Browse Product Catalogue

`GET /inventory?page=0&size=20`

Authentication: JWT

Authorization: `CUSTOMER` or `STORE_MANAGER`

Returns a paginated list of products from Couchbase.

Success:

HTTP `200 OK`

    {
      "content": [
        {
          "productId": "P1001",
          "productName": "Example Product",
          "productType": "STANDARD",
          "availableCount": 15,
          "availableQuantity": 15,
          "available": true,
          "updatedAt": "2025-01-01T00:00:00Z"
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1
    }

Failure:

- `400 Bad Request` — invalid pagination parameters.
- `401 Unauthorized` — missing or invalid JWT.
- `403 Forbidden` — authenticated user does not have an allowed role.

### Query API Rules

- Both APIs read from Couchbase and must not query PostgreSQL directly.
- `page` is zero-based and defaults to `0`.
- `size` defaults to `20` and must be greater than zero.
- The service must enforce a maximum page size of `100`.
- Catalogue results are ordered by `updatedAt` descending, using the event `occurredAt` timestamp.
- Inventory update events do not carry a version in Flow 3; versioning is deferred to Flow 3.5.
- Query results may temporarily lag behind the PostgreSQL source of truth because the read model is updated asynchronously.


## 9. Increase Inventory

`POST /inventory/increase`

### Authentication

JWT required.

### Authorization

`STORE_MANAGER` only.

### Request

```
{
  "productId": "P1001",
  "quantity": 10
}
```

### Success Response

HTTP `200 OK`

```
{
  "productId": "P1001",
  "availableQuantity": 25,
  "version": 4
}
```

The values above are illustrative. The actual response must contain the resulting quantity and version from the committed inventory operation.

### Error Responses

* `400 Bad Request` — missing or invalid request fields, or quantity less than or equal to zero.
* `401 Unauthorized` — missing or invalid JWT.
* `403 Forbidden` — caller does not have the `STORE_MANAGER` role.
* `404 Not Found` — product does not exist.

### Rules

* Inventory addition is performed by Inventory Service.
* PostgreSQL is the source of truth.
* Quantity and version are updated atomically.
* A successful inventory change produces a versioned Outbox event.
* A rejected request must not change inventory or create an event.
* The event is published asynchronously to `InventoryUpdates`.
* The response must not depend on Couchbase catching up with the committed change.
