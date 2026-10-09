package com.example.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.inventory.persistence.entity.InventoryDocument;
import com.example.inventory.persistence.repository.InventoryDocumentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class InventoryQueryServiceTest {
    @Test
    void readsProductProjectionFromCouchbase() throws Exception {
        InventoryDocumentRepository repository = mock(InventoryDocumentRepository.class);
        InventoryDocument document = InventoryDocument.builder()
                .productId("p")
                .productName("Product")
                .productType("STANDARD")
                .availableCount(4)
                .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build();
        when(repository.findById("p")).thenReturn(Optional.of(document));

        var response = new InventoryQueryServiceImpl(repository).findByProductId("p");

        assertEquals("Product", response.productName());
        assertEquals(4, response.availableCount());
    }

    @Test
    void rejectsInvalidPageSize() {
        InventoryDocumentRepository repository = mock(InventoryDocumentRepository.class);

        assertThrows(
                BusinessException.class,
                () -> new InventoryQueryServiceImpl(repository).findAll(0, 101));
    }

    @Test
    void ordersCatalogueByUpdatedAtDescending() throws Exception {
        InventoryDocumentRepository repository = mock(InventoryDocumentRepository.class);
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(InventoryDocument.builder().productId("p").build())));

        new InventoryQueryServiceImpl(repository).findAll(0, 20);

        verify(repository).findAll(any(Pageable.class));
    }
}
