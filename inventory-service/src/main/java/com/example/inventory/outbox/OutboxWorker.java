package com.example.inventory.outbox;

import com.example.inventory.persistence.entity.OutboxEvent;
import com.example.inventory.persistence.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OutboxWorker {
    private static final int MAX_RETRIES = 4;

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final TransactionTemplate transactions;
    private final int batchSize;
    private final long leaseSeconds;

    public OutboxWorker(
            OutboxEventRepository outboxRepository,
            KafkaTemplate<String, String> kafka,
            ObjectMapper mapper,
            TransactionTemplate transactions,
            @Value("${outbox.batch-size:20}") int batchSize,
            @Value("${outbox.lease-seconds:60}") long leaseSeconds) {
        this.outboxRepository = outboxRepository;
        this.kafka = kafka;
        this.mapper = mapper;
        this.transactions = transactions;
        this.batchSize = batchSize;
        this.leaseSeconds = leaseSeconds;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-ms:1000}")
    public void scheduledPublish() {
        publishOnce();
    }

    public void publishOnce() {
        recoverExhaustedLeases();
        for (Event event : claim()) {
            try {
                kafka.send(event.topic(), event.productId(), payload(event)).get(30, TimeUnit.SECONDS);
                markPublished(event.eventId());
            } catch (Exception ex) {
                markFailed(event.eventId(), rootMessage(ex));
            }
        }
    }

    /**
     * Claims a batch in a short transaction, then releases database locks before Kafka I/O.
     */
    protected List<Event> claim() {
        return transactions.execute(
                status -> {
                    List<OutboxEvent> candidates =
                            outboxRepository.claimCandidates(MAX_RETRIES + 1, batchSize);
                    Instant leaseUntil = Instant.now().plusSeconds(leaseSeconds);
                    List<Event> events =
                            candidates
                                    .stream()
                                    .map(
                                            event ->
                                                    new Event(
                                                            event.getEventId(),
                                                            event.getProductId(),
                                                            event.getProductName(),
                                                            event.getProductType(),
                                                            event.getPreviousCount(),
                                                            event.getCurrentCount(),
                                                            event.getVersion(),
                                                            event.getOccurredAt(),
                                                            event.getTopic(),
                                                            event.getRetryCount()))
                                    .toList();
                    candidates.forEach(
                            event -> {
                                event.markProcessing(leaseUntil);
                                outboxRepository.save(event);
                            });
                    return events;
                });
    }

    private void recoverExhaustedLeases() {
        transactions.executeWithoutResult(
                status ->
                        outboxRepository.recoverExhaustedLeases(
                                Instant.now(), MAX_RETRIES + 1, "retry limit reached after worker lease expired"));
    }

    private void markPublished(UUID id) {
        transactions.executeWithoutResult(
                status ->
                        outboxRepository
                                .findById(id)
                                .ifPresent(
                                        event -> {
                                            if (event.getStatus()
                                                    == com.example.inventory.persistence.entity.OutboxStatus.PROCESSING) {
                                                event.markPublished(Instant.now());
                                                outboxRepository.save(event);
                                            }
                                        }));
    }

    private void markFailed(UUID id, String error) {
        transactions.executeWithoutResult(
                status ->
                        outboxRepository
                                .findById(id)
                                .ifPresent(
                                        event -> {
                                            if (event.getStatus()
                                                    == com.example.inventory.persistence.entity.OutboxStatus.PROCESSING) {
                                                event.markFailed(error, MAX_RETRIES);
                                                outboxRepository.save(event);
                                            }
                                        }));
    }

    private String payload(Event event) {
        try {
            return mapper.writeValueAsString(
                    new Payload(
                            event.eventId(),
                            event.productId(),
                            event.productName(),
                            event.productType(),
                            event.previous(),
                            event.current(),
                            event.version(),
                            event.occurredAt() == null ? Instant.now() : event.occurredAt(),
                            event.topic()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String rootMessage(Exception ex) {
        Throwable t = ex;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        String message = String.valueOf(t.getMessage());
        return message.substring(0, Math.min(1000, message.length()));
    }

    public record Event(
            UUID eventId,
            String productId,
            String productName,
            String productType,
            int previous,
            int current,
            long version,
            Instant occurredAt,
            String topic,
            int retryCount) {
        public Event(UUID eventId, String productId, int previous, int current, String topic, int retryCount) {
            this(eventId, productId, null, null, previous, current, 0, null, topic, retryCount);
        }

        public Event(
                UUID eventId,
                String productId,
                String productName,
                String productType,
                int previous,
                int current,
                String topic,
                int retryCount) {
            this(eventId, productId, productName, productType, previous, current, 0, null, topic, retryCount);
        }
    }

    record Payload(
            UUID eventId,
            String productId,
            String productName,
            String productType,
            int previousCount,
            int currentCount,
            long version,
            Instant occurredAt,
            String topic) {
        Payload(
                UUID eventId,
                String productId,
                String productName,
                String productType,
                int previousCount,
                int currentCount,
                String topic,
                Instant occurredAt) {
            this(eventId, productId, productName, productType, previousCount, currentCount, 0, occurredAt, topic);
        }
    }
}
