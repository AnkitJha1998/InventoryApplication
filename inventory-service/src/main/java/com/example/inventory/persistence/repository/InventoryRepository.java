package com.example.inventory.persistence.repository;

import com.example.inventory.persistence.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, String> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = "UPDATE inventory SET available_count = available_count - :quantity, "
                    + "version = version + 1, updated_at = CURRENT_TIMESTAMP "
                    + "WHERE product_id = :productId AND available_count >= :quantity",
            nativeQuery = true)
    int reduceAvailableCount(
            @Param("productId") String productId,
            @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = "UPDATE inventory SET available_count = available_count + :quantity, "
                    + "version = version + 1, updated_at = CURRENT_TIMESTAMP "
                    + "WHERE product_id = :productId",
            nativeQuery = true)
    int increaseAvailableCount(
            @Param("productId") String productId,
            @Param("quantity") int quantity);
}
