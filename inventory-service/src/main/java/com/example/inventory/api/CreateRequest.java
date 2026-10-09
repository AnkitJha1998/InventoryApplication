package com.example.inventory.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateRequest(
        @NotBlank String productId,
        @NotBlank String productName,
        @NotBlank String productType,
        @NotNull @Positive Integer quantity) {
}
