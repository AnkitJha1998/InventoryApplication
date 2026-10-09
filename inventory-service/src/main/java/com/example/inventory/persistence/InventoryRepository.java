package com.example.inventory.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, String> {
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          "UPDATE inventory SET available_count = available_count - :quantity, "
              + "updated_at = CURRENT_TIMESTAMP "
              + "WHERE product_id = :productId AND available_count >= :quantity",
      nativeQuery = true)
  int reduceAvailableCount(@Param("productId") String productId, @Param("quantity") int quantity);
}
