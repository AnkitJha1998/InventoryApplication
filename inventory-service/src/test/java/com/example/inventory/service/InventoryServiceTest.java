package com.example.inventory.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
