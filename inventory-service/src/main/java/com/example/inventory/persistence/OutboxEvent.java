package com.example.inventory.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "previous_count", nullable = false)
    private int previousCount;

    @Column(name = "current_count", nullable = false)
    private int currentCount;

    @Column(nullable = false, length = 64)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "lease_until")
    private Instant leaseUntil;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(UUID eventId, String productId, int previousCount, int currentCount, String topic) {
        this.eventId = eventId;
        this.productId = productId;
        this.previousCount = previousCount;
        this.currentCount = currentCount;
        this.topic = topic;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getProductId() {
        return productId;
    }

    public int getPreviousCount() {
        return previousCount;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public String getTopic() {
        return topic;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void markProcessing(Instant leaseUntil) {
        if (status == OutboxStatus.PROCESSING) {
            retryCount++;
        }
        status = OutboxStatus.PROCESSING;
        this.leaseUntil = leaseUntil;
    }

    public void markPublished(Instant publishedAt) {
        status = OutboxStatus.PUBLISHED;
        leaseUntil = null;
        lastError = null;
        this.publishedAt = publishedAt;
    }

    public void markFailed(String error, int maxRetries) {
        retryCount++;
        status = retryCount > maxRetries ? OutboxStatus.FAILED : OutboxStatus.PENDING;
        leaseUntil = null;
        lastError = error;
    }

    public void markFailed(String error) {
        markFailed(error, 4);
    }

    public void exhaustLease(String error) {
        status = OutboxStatus.FAILED;
        leaseUntil = null;
        if (lastError == null) {
            lastError = error;
        }
    }
}
