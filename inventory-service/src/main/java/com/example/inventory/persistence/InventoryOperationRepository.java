package com.example.inventory.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryOperationRepository extends JpaRepository<InventoryOperation, UUID> {
  Optional<InventoryOperation> findByOrderIdAndProductId(String orderId, String productId);
}
