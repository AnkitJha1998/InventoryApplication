package com.example.notification.persistence.repository;

import com.example.notification.persistence.entity.BackInStockRequest;
import com.example.notification.persistence.entity.BackInStockStatus;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BackInStockRequestRepository extends JpaRepository<BackInStockRequest, UUID> {
    boolean existsByCustomerIdAndProductIdAndStatus(UUID customerId, String productId, BackInStockStatus status);

    List<BackInStockRequest> findByProductIdAndStatus(String productId, BackInStockStatus status);
}
