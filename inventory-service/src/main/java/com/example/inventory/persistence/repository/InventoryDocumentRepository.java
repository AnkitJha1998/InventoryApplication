package com.example.inventory.persistence.repository;

import com.example.inventory.persistence.entity.InventoryDocument;
import org.springframework.data.couchbase.repository.CouchbaseRepository;

public interface InventoryDocumentRepository extends CouchbaseRepository<InventoryDocument, String> {
}
