package com.example.inventory.service;

import com.example.inventory.api.ReductionRequest;
import com.example.inventory.persistence.InventoryOperation;
import com.example.inventory.persistence.InventoryOperationRepository;
import com.example.inventory.persistence.InventoryRepository;
import com.example.inventory.persistence.OutboxEventRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryServiceTest {
    @Test
    void sameOperationIsIdempotent() {
        InventoryRepository inventory = mock(InventoryRepository.class);
        InventoryOperationRepository operations = mock(InventoryOperationRepository.class);
        when(operations.findByOrderIdAndProductId("o", "p"))
                .thenReturn(Optional.of(new InventoryOperation(UUID.randomUUID(), "o", "p", 2, 10, 8)));

        var response = new InventoryServiceImpl(inventory, operations, mock(OutboxEventRepository.class))
                .reduce(new ReductionRequest("o", "p", 2));

        assertTrue(response.alreadyProcessed());
    }

    @Test
    void conflictingQuantityIsRejected() {
        InventoryRepository inventory = mock(InventoryRepository.class);
        InventoryOperationRepository operations = mock(InventoryOperationRepository.class);
        when(operations.findByOrderIdAndProductId(eq("o"), eq("p")))
                .thenReturn(Optional.of(new InventoryOperation(UUID.randomUUID(), "o", "p", 2, 10, 8)));

        assertThrows(BusinessException.class,
                () -> new InventoryServiceImpl(inventory, operations, mock(OutboxEventRepository.class))
                        .reduce(new ReductionRequest("o", "p", 3)));
    }

    @Test
    void reducesStockAndWritesOutboxEvents() {
        InventoryRepository inventory = mock(InventoryRepository.class);
        InventoryOperationRepository operations = mock(InventoryOperationRepository.class);
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        when(operations.findByOrderIdAndProductId("o", "p")).thenReturn(Optional.empty());
        when(inventory.reduceAvailableCount("p", 2)).thenReturn(1);
        when(inventory.findById("p")).thenReturn(Optional.of(new com.example.inventory.persistence.Inventory(
                "p", "Product", "STANDARD", 3)));

        var response = new InventoryServiceImpl(inventory, operations, outbox)
                .reduce(new ReductionRequest("o", "p", 2));

        assertTrue(!response.alreadyProcessed());
        org.mockito.Mockito.verify(operations).save(any());
        org.mockito.Mockito.verify(outbox).save(any());
    }
}
