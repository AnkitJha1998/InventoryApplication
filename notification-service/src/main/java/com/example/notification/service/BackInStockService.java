package com.example.notification.service;

import com.example.notification.api.BackInStockRequestBody;
import com.example.notification.persistence.entity.BackInStockRequest;
import com.example.notification.persistence.entity.BackInStockStatus;
import com.example.notification.persistence.repository.BackInStockRequestRepository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BackInStockService {
    private final BackInStockRequestRepository requestRepository;
    private final InventoryQueryClient inventoryQueryClient;

    public BackInStockService(BackInStockRequestRepository requestRepository, InventoryQueryClient inventoryQueryClient) {
        this.requestRepository = requestRepository;
        this.inventoryQueryClient = inventoryQueryClient;
    }

    @Transactional(rollbackFor = NotificationException.class)
    public BackInStockRequest request(UUID customerId, BackInStockRequestBody body, String authorization) throws NotificationException {
        if (body == null || body.productId() == null || body.productId().isBlank()) {
            throw new NotificationException("productId is required");
        }
        InventoryQueryClient.InventoryProduct product = inventoryQueryClient.query(body.productId(), authorization);
        if (product.quantity() > 0) {
            throw new NotificationException("product is already in stock");
        }
        if (requestRepository.existsByCustomerIdAndProductIdAndStatus(customerId, body.productId(), BackInStockStatus.ACTIVE)) {
            throw new NotificationException("an active request already exists");
        }
        Instant now = Instant.now();
        BackInStockRequest request = BackInStockRequest.builder()
                .requestId(UUID.randomUUID())
                .customerId(customerId)
                .productId(body.productId())
                .status(BackInStockStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            return requestRepository.save(request);
        } catch (DataIntegrityViolationException exception) {
            throw new NotificationException("an active request already exists", exception);
        }
    }
}
