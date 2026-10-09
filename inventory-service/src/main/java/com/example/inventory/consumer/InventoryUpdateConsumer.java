package com.example.inventory.consumer;

import com.example.inventory.persistence.entity.InventoryDocument;
import com.example.inventory.persistence.repository.InventoryDocumentRepository;
import com.example.inventory.persistence.repository.InventoryDocumentWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class InventoryUpdateConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryUpdateConsumer.class);

    private final InventoryDocumentRepository repository;
    private final InventoryDocumentWriter writer;
    private final ObjectMapper objectMapper;

    public InventoryUpdateConsumer(
            InventoryDocumentRepository repository,
            ObjectMapper objectMapper) {
        this(repository, objectMapper, new RepositoryInventoryDocumentWriter(repository));
    }

    @Autowired
    public InventoryUpdateConsumer(
            InventoryDocumentRepository repository,
            ObjectMapper objectMapper,
            InventoryDocumentWriter writer) {
        this.repository = repository;
        this.writer = writer;
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
            writer.applyIfNewer(
                    InventoryDocument.builder()
                            .productId(event.productId())
                            .productName(event.productName())
                            .productType(event.productType())
                            .availableCount(event.currentCount())
                            .availableQuantity(event.currentCount())
                            .available(event.currentCount() > 0)
                            .version(event.version())
                            .updatedAt(event.occurredAt())
                            .build());
        } catch (RuntimeException exception) {
            LOGGER.error("Unable to persist InventoryUpdate read model", exception);
            throw exception;
        }
        acknowledgment.acknowledge();
    }

    private static final class RepositoryInventoryDocumentWriter
            implements InventoryDocumentWriter {
        private final InventoryDocumentRepository repository;

        private RepositoryInventoryDocumentWriter(InventoryDocumentRepository repository) {
            this.repository = repository;
        }

        @Override
        public void applyIfNewer(InventoryDocument document) {
            Optional<InventoryDocument> current = repository.findById(document.getProductId());
            if (current.isEmpty() || document.getVersion() > current.get().getVersion()) {
                repository.save(document);
            }
        }
    }
}
