package com.example.inventory.api;

import java.util.List;

public record InventoryPageResponse(
        List<InventoryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
