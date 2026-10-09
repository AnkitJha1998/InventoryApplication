package com.example.inventory.consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.example.inventory.persistence.entity.InventoryDocument;
import com.example.inventory.persistence.repository.InventoryDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

class InventoryUpdateConsumerTest {
    @Test
    void ignoresEqualAndOlderVersions() {
        InventoryDocumentRepository repository = mock(InventoryDocumentRepository.class);
        when(repository.findById("p"))
                .thenReturn(Optional.of(InventoryDocument.builder().productId("p").version(3).build()));
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        var consumer = new InventoryUpdateConsumer(repository, new ObjectMapper());

        String event = "{\"eventId\":\"" + UUID.randomUUID()
                + "\",\"productId\":\"p\",\"currentCount\":2,\"version\":3} ";
        consumer.consume(event, acknowledgment);
        consumer.consume(event.replace("\"version\":3", "\"version\":2"), acknowledgment);

        verify(repository, never()).save(any());
        verify(acknowledgment, org.mockito.Mockito.times(2)).acknowledge();
    }

    @Test
    void acknowledgesOnlyAfterReadModelWrite() {
        InventoryDocumentRepository repository = mock(InventoryDocumentRepository.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        var consumer = new InventoryUpdateConsumer(repository, new ObjectMapper());

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

        verify(repository).save(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void doesNotAcknowledgeTransientCouchbaseFailure() {
        InventoryDocumentRepository repository = mock(InventoryDocumentRepository.class);
        when(repository.save(any())).thenThrow(new IllegalStateException("temporarily unavailable"));
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        var consumer = new InventoryUpdateConsumer(repository, new ObjectMapper());

        assertThrows(
                IllegalStateException.class,
                () -> consumer.consume("{\"productId\":\"p\"}", acknowledgment));
        verify(acknowledgment, never()).acknowledge();
    }
}
