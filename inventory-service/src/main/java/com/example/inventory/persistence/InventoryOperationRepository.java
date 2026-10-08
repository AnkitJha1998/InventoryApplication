package com.example.inventory.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InventoryOperationRepository extends JpaRepository<InventoryOperation, UUID> {
    Optional<InventoryOperation> findByOrderIdAndProductId(String orderId, String productId);
}
