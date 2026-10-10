package com.example.inventory.consumer;

import com.example.inventory.persistence.entity.InventoryDocument;
import com.example.inventory.persistence.repository.InventoryDocumentWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InventoryUpdateConsumer {
  private final InventoryDocumentWriter writer;
  private final ObjectMapper objectMapper;

  public InventoryUpdateConsumer(InventoryDocumentWriter writer, ObjectMapper objectMapper) {
    this.writer = writer;
    this.objectMapper = objectMapper;
    this.objectMapper.findAndRegisterModules();
  }

  @KafkaListener(
      topics = "InventoryUpdate",
      groupId = "${inventory.query.consumer-group:inventory-query-read-model}")
  public void consume(String payload, Acknowledgment acknowledgment) {
    log.info("[consume][{}] consumer entry", Instant.now());

    final InventoryUpdate event;
    try {
      event = objectMapper.readValue(payload, InventoryUpdate.class);
    } catch (JsonProcessingException exception) {
      log.error("[consume][{}] unable to parse InventoryUpdate event", Instant.now(), exception);
      return;
    }

    InventoryDocument document =
        InventoryDocument.builder()
            .productId(event.productId())
            .productName(event.productName())
            .productType(event.productType())
            .availableCount(event.currentCount())
            .availableQuantity(event.currentCount())
            .available(event.currentCount() > 0)
            .version(event.version())
            .updatedAt(event.occurredAt())
            .build();

    try {
      writer.applyIfNewer(document);
    } catch (RuntimeException exception) {
      log.error(
          "[consume][{}] unable to persist InventoryUpdate read model productId={}",
          Instant.now(),
          event.productId(),
          exception);
      throw exception;
    }

    acknowledgment.acknowledge();
    log.info(
        "[consume][{}] InventoryUpdate processed eventId={} productId={}",
        Instant.now(),
        event.eventId(),
        event.productId());
  }
}
