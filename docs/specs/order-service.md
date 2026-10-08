# Order Service Specification

## Responsibility

The Order Service provides a simulated order creation endpoint.

Order creation is limited to demonstrating the interaction between Order Service and Inventory Service.

Full order persistence and order lifecycle management are outside the scope of this demo.

## API

### Create Order

`POST /orders`

The endpoint requires a valid JWT for a Customer.

The Order Service validates the JWT and authorizes the request based on the Customer role.

The Order Service calls the Inventory Service to reduce inventory for the requested product and quantity.

The same user JWT is forwarded to the Inventory Service.

## Flow

    Consumer
        |
        | POST /orders
        | Authorization: Bearer <JWT>
        v
    Order Service
        |
        | POST /inventory/reduce
        | Authorization: Bearer <JWT>
        v
    Inventory Service
        |
        v
    PostgreSQL

## Rules

- The Order Service is the consumer-facing entry point for order creation.
- A valid Customer JWT is required to create an order.
- The Order Service must validate the JWT.
- The Order Service must authorize the request using the Customer role.
- The Order Service must call the Inventory Service to reduce inventory.
- The same user JWT must be forwarded to the Inventory Service.
- The Inventory Service independently validates the forwarded JWT.
- The Order Service must not modify inventory directly.
- The Order Service must not access the Inventory Service database.
- Inventory availability is determined by the Inventory Service.
- If inventory reduction succeeds, the Order Service returns a successful order response.
- If inventory reduction fails, the Order Service returns an appropriate failure response.
- Order persistence and order lifecycle management are outside the scope of this demo.

## Persistence

No order persistence is required for this flow.