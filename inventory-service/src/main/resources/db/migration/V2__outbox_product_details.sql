ALTER TABLE outbox_events
    ADD COLUMN product_name VARCHAR(255),
    ADD COLUMN product_type VARCHAR(100);
