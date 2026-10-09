package com.example.inventory.consumer;

import com.example.inventory.persistence.entity.InventoryDocument;
import com.example.inventory.persistence.repository.InventoryDocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class InventoryUpdateConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryUpdateConsumer.class);

    private final InventoryDocumentRepository repository;
    private final ObjectMapper objectMapper;

    public InventoryUpdateConsumer(
            InventoryDocumentRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.objectMapper.findAndRegisterModules();
    }

    @KafkaListener(
            topics = "InventoryUpdate",
            groupId = "${inventory.query.consumer-group:inventory-query-read-model}")
    public void consume(String payload, Acknowledgment acknowledgment) {
        final InventoryUpdate event;
        try {
            event = objectMapper.readValue(payload, InventoryUpdate.class);
        } catch (JsonProcessingException exception) {
            LOGGER.error("Unable to parse InventoryUpdate event", exception);
            return;
        }

        try {
            repository.save(
                    InventoryDocument.builder()
                            .productId(event.productId())
                            .productName(event.productName())
                            .productType(event.productType())
                            .availableCount(event.currentCount())
                            .availableQuantity(event.currentCount())
                            .available(event.currentCount() > 0)
                            .updatedAt(event.occurredAt())
                            .build());
        } catch (RuntimeException exception) {
            LOGGER.error("Unable to persist InventoryUpdate read model", exception);
            throw exception;
        }
        acknowledgment.acknowledge();
    }
}
