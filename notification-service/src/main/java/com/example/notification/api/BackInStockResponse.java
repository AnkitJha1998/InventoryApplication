package com.example.notification.api;

import com.example.notification.persistence.entity.BackInStockRequest;

import java.util.UUID;

public record BackInStockResponse(UUID requestId, String productId, String status) {
    public static BackInStockResponse from(BackInStockRequest request) {
        return new BackInStockResponse(request.getRequestId(), request.getProductId(), request.getStatus().name());
    }
}
