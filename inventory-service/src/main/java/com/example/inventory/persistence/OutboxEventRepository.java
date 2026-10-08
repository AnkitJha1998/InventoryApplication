package com.example.inventory.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = "SELECT * FROM outbox_events "
            + "WHERE retry_count < :maxRetries "
            + "AND (status = 'PENDING' OR (status = 'PROCESSING' AND lease_until < CURRENT_TIMESTAMP)) "
            + "ORDER BY created_at LIMIT :batchSize FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> claimCandidates(@Param("maxRetries") int maxRetries, @Param("batchSize") int batchSize);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = com.example.inventory.persistence.OutboxStatus.FAILED, "
            + "e.leaseUntil = null, e.lastError = COALESCE(e.lastError, :message) "
            + "WHERE e.status = com.example.inventory.persistence.OutboxStatus.PROCESSING "
            + "AND e.leaseUntil < :now AND e.retryCount >= :maxRetries")
    int recoverExhaustedLeases(@Param("now") Instant now, @Param("maxRetries") int maxRetries,
                               @Param("message") String message);
}
