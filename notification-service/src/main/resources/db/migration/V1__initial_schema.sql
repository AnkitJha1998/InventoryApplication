CREATE TABLE back_in_stock_requests (
    request_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uq_back_in_stock_active_customer_product
    ON back_in_stock_requests(customer_id, product_id)
    WHERE status = 'ACTIVE';
CREATE INDEX idx_back_in_stock_product_status
    ON back_in_stock_requests(product_id, status);

CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    request_id UUID NOT NULL REFERENCES back_in_stock_requests(request_id),
    event_id UUID NOT NULL,
    message VARCHAR(512) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('CREATED', 'NOTIFIED')),
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_notifications_event_request
    ON notifications(event_id, request_id);
CREATE INDEX idx_notifications_customer ON notifications(customer_id, created_at, notification_id);

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
