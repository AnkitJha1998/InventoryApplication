package com.example.inventory.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record IncreaseRequest(
        @NotBlank String productId,
        @NotNull @Positive Integer quantity) {
}
