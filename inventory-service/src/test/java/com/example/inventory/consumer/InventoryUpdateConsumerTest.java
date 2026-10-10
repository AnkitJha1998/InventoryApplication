package com.example.inventory.consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.inventory.persistence.entity.InventoryDocument;
import com.example.inventory.persistence.repository.InventoryDocumentWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

class InventoryUpdateConsumerTest {
  @Test
  void delegatesEqualAndOlderVersionsToAtomicWriter() {
    InventoryDocumentWriter writer = mock(InventoryDocumentWriter.class);
    Acknowledgment acknowledgment = mock(Acknowledgment.class);
    var consumer = new InventoryUpdateConsumer(writer, new ObjectMapper());

    String event =
        "{\"eventId\":\""
            + UUID.randomUUID()
            + "\",\"productId\":\"p\",\"currentCount\":2,\"version\":3}";
    consumer.consume(event, acknowledgment);
    consumer.consume(event.replace("\"version\":3", "\"version\":2"), acknowledgment);

    verify(writer, times(2)).applyIfNewer(any(InventoryDocument.class));
    verify(acknowledgment, times(2)).acknowledge();
  }

  @Test
  void acknowledgesOnlyAfterReadModelWrite() {
    InventoryDocumentWriter writer = mock(InventoryDocumentWriter.class);
    Acknowledgment acknowledgment = mock(Acknowledgment.class);
    var consumer = new InventoryUpdateConsumer(writer, new ObjectMapper());

    consumer.consume(
        "{\"eventId\":\""
            + UUID.randomUUID()
            + "\",\"productId\":\"p\",\"productName\":\"Product\","
            + "\"productType\":\"STANDARD\",\"previousCount\":1,"
            + "\"currentCount\":2,\"topic\":\"InventoryUpdate\","
            + "\"occurredAt\":\""
            + Instant.now()
            + "\"}",
        acknowledgment);

    verify(writer).applyIfNewer(any(InventoryDocument.class));
    verify(acknowledgment).acknowledge();
  }

  @Test
  void doesNotAcknowledgeTransientCouchbaseFailure() {
    InventoryDocumentWriter writer = mock(InventoryDocumentWriter.class);
    doThrow(new IllegalStateException("temporarily unavailable"))
        .when(writer)
        .applyIfNewer(any(InventoryDocument.class));
    Acknowledgment acknowledgment = mock(Acknowledgment.class);
    var consumer = new InventoryUpdateConsumer(writer, new ObjectMapper());

    assertThrows(
        IllegalStateException.class,
        () -> consumer.consume("{\"productId\":\"p\"}", acknowledgment));
    verify(acknowledgment, never()).acknowledge();
  }
}
