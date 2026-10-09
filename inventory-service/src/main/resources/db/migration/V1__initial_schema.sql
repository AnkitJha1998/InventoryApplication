CREATE TABLE inventory (
  product_id VARCHAR(255) PRIMARY KEY,
  product_name VARCHAR(255) NOT NULL,
  product_type VARCHAR(100) NOT NULL,
  available_count INTEGER NOT NULL CHECK (available_count >= 0),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventory_operations (
  operation_id UUID PRIMARY KEY,
  order_id VARCHAR(255) NOT NULL,
  product_id VARCHAR(255) NOT NULL REFERENCES inventory(product_id),
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  previous_count INTEGER NOT NULL CHECK (previous_count >= 0),
  current_count INTEGER NOT NULL CHECK (current_count >= 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_inventory_operation_order_product UNIQUE (order_id, product_id)
);
CREATE INDEX idx_inventory_operations_product ON inventory_operations(product_id);

CREATE TABLE outbox_events (
  event_id UUID PRIMARY KEY,
  product_id VARCHAR(255) NOT NULL REFERENCES inventory(product_id),
  previous_count INTEGER NOT NULL CHECK (previous_count >= 0),
  current_count INTEGER NOT NULL CHECK (current_count >= 0),
  topic VARCHAR(64) NOT NULL CHECK (topic IN ('InventoryUpdate', 'NotificationTrigger')),
  status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','PROCESSING','PUBLISHED','FAILED')),
  retry_count INTEGER NOT NULL DEFAULT 0 CHECK (retry_count >= 0),
  lease_until TIMESTAMPTZ,
  last_error VARCHAR(1000),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  published_at TIMESTAMPTZ
);
CREATE INDEX idx_outbox_claim ON outbox_events(status, retry_count, lease_until, created_at);
