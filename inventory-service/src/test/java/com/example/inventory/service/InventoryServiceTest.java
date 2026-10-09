package com.example.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.inventory.api.CreateRequest;
import com.example.inventory.api.IncreaseRequest;
import com.example.inventory.api.ReductionRequest;
import com.example.inventory.persistence.entity.InventoryOperation;
import com.example.inventory.persistence.repository.InventoryOperationRepository;
import com.example.inventory.persistence.repository.InventoryRepository;
import com.example.inventory.persistence.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {
  @Test
  void sameOperationIsIdempotent() throws Exception {
    InventoryRepository inventory = mock(InventoryRepository.class);
    InventoryOperationRepository operations = mock(InventoryOperationRepository.class);
    when(operations.findByOrderIdAndProductId("o", "p"))
        .thenReturn(
            Optional.of(
                InventoryOperation.builder()
                    .operationId(UUID.randomUUID())
                    .orderId("o")
                    .productId("p")
                    .quantity(2)
                    .previousCount(10)
                    .currentCount(8)
                    .createdAt(Instant.now())
                    .build()));

    var response =
        new InventoryServiceImpl(inventory, operations, mock(OutboxEventRepository.class))
            .reduce(new ReductionRequest("o", "p", 2));

    assertTrue(response.alreadyProcessed());
  }

  @Test
  void managerIncreaseUpdatesQuantityVersionAndOutbox() throws Exception {
    InventoryRepository inventory = mock(InventoryRepository.class);
    InventoryOperationRepository operations = mock(InventoryOperationRepository.class);
    OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    when(inventory.increaseAvailableCount("p", 2)).thenReturn(1);
    when(inventory.findById("p"))
        .thenReturn(
            Optional.of(
                com.example.inventory.persistence.entity.Inventory.builder()
                    .productId("p")
                    .productName("Product")
                    .productType("STANDARD")
                    .availableCount(7)
                    .version(1)
                    .updatedAt(Instant.now())
                    .build()));

    var response =
        new InventoryServiceImpl(inventory, operations, outbox)
            .increase(new IncreaseRequest("p", 2));

    org.junit.jupiter.api.Assertions.assertEquals(7, response.availableQuantity());
    org.junit.jupiter.api.Assertions.assertEquals(1, response.version());
    org.mockito.Mockito.verify(outbox).save(any());
  }

  @Test
  void conflictingQuantityIsRejected() {
    InventoryRepository inventory = mock(InventoryRepository.class);
    InventoryOperationRepository operations = mock(InventoryOperationRepository.class);
    when(operations.findByOrderIdAndProductId(eq("o"), eq("p")))
        .thenReturn(
            Optional.of(
                InventoryOperation.builder()
                    .operationId(UUID.randomUUID())
                    .orderId("o")
                    .productId("p")
                    .quantity(2)
                    .previousCount(10)
                    .currentCount(8)
                    .createdAt(Instant.now())
                    .build()));

    assertThrows(
        BusinessException.class,
        () ->
            new InventoryServiceImpl(inventory, operations, mock(OutboxEventRepository.class))
                .reduce(new ReductionRequest("o", "p", 3)));
  }

  @Test
  void createsInitialInventoryVersionAndOutboxEvent() throws Exception {
    InventoryRepository inventory = mock(InventoryRepository.class);
    OutboxEventRepository outbox = mock(OutboxEventRepository.class);

    var response =
        new InventoryServiceImpl(inventory, mock(InventoryOperationRepository.class), outbox)
            .create(new CreateRequest("p", "Product", "STANDARD", 5));

    assertEquals(1, response.version());
    assertEquals(5, response.availableQuantity());
    org.mockito.ArgumentCaptor<com.example.inventory.persistence.entity.OutboxEvent> event =
        org.mockito.ArgumentCaptor.forClass(
            com.example.inventory.persistence.entity.OutboxEvent.class);
    org.mockito.Mockito.verify(outbox).save(event.capture());
    assertEquals(1, event.getValue().getVersion());
    assertEquals("p", event.getValue().getProductId());
    assertEquals("Product", event.getValue().getProductName());
    assertEquals("STANDARD", event.getValue().getProductType());
    assertEquals(5, event.getValue().getCurrentCount());
    assertEquals("InventoryUpdate", event.getValue().getTopic());
    org.junit.jupiter.api.Assertions.assertNotNull(event.getValue().getEventId());
    org.junit.jupiter.api.Assertions.assertNotNull(event.getValue().getOccurredAt());
  }

  @Test
  void duplicateCreationIsRejectedWithoutWritingOutbox() {
    InventoryRepository inventory = mock(InventoryRepository.class);
    OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    when(inventory.existsById("p")).thenReturn(true);

    assertThrows(
        BusinessException.class,
        () ->
            new InventoryServiceImpl(inventory, mock(InventoryOperationRepository.class), outbox)
                .create(new CreateRequest("p", "Product", "STANDARD", 5)));
    org.mockito.Mockito.verify(inventory, org.mockito.Mockito.never()).save(any());
    org.mockito.Mockito.verifyNoInteractions(outbox);
  }

  @Test
  void invalidCreationDoesNotWriteInventoryOrOutbox() {
    InventoryRepository inventory = mock(InventoryRepository.class);
    OutboxEventRepository outbox = mock(OutboxEventRepository.class);

    assertThrows(
        BusinessException.class,
        () ->
            new InventoryServiceImpl(inventory, mock(InventoryOperationRepository.class), outbox)
                .create(new CreateRequest("p", "Product", "STANDARD", 0)));
    org.mockito.Mockito.verifyNoInteractions(inventory, outbox);
  }

  @Test
  void reducesStockAndWritesOutboxEvents() throws Exception {
    InventoryRepository inventory = mock(InventoryRepository.class);
    InventoryOperationRepository operations = mock(InventoryOperationRepository.class);
    OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    when(operations.findByOrderIdAndProductId("o", "p")).thenReturn(Optional.empty());
    when(inventory.reduceAvailableCount("p", 2)).thenReturn(1);
    when(inventory.findById("p"))
        .thenReturn(
            Optional.of(
                com.example.inventory.persistence.entity.Inventory.builder()
                    .productId("p")
                    .productName("Product")
                    .productType("STANDARD")
                    .availableCount(3)
                    .updatedAt(Instant.now())
                    .build()));

    var response =
        new InventoryServiceImpl(inventory, operations, outbox)
            .reduce(new ReductionRequest("o", "p", 2));

    assertTrue(!response.alreadyProcessed());
    org.mockito.Mockito.verify(operations).save(any());
    org.mockito.Mockito.verify(outbox).save(any());
  }
}
