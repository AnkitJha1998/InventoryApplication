package com.example.inventory.persistence.repository;

import com.example.inventory.persistence.entity.InventoryDocument;

public interface InventoryDocumentWriter {
    void applyIfNewer(InventoryDocument document);
}
