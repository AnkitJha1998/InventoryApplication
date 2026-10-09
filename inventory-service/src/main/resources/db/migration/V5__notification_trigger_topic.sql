ALTER TABLE outbox_events
    DROP CONSTRAINT IF EXISTS outbox_events_topic_check;

ALTER TABLE outbox_events
    ADD CONSTRAINT outbox_events_topic_check
    CHECK (topic IN ('InventoryUpdate', 'NotificationTrigger'));
