package com.example.inventory.api;

public record ReductionResponse(String orderId, String productId, int quantity,
                                int previousCount, int currentCount, boolean alreadyProcessed) {}
