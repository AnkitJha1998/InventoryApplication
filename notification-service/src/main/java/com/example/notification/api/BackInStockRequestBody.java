package com.example.notification.api;

import jakarta.validation.constraints.NotBlank;

public record BackInStockRequestBody(@NotBlank(message = "productId is required") String productId) {
}
