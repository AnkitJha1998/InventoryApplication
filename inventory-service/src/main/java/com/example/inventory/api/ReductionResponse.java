package com.example.inventory.api;

public record ReductionResponse(
        String orderId,
        String productId,
        int quantity,
        int previousCount,
        int currentCount,
        boolean alreadyProcessed,
        long version) {
    public ReductionResponse(
            String orderId,
            String productId,
            int quantity,
            int previousCount,
            int currentCount,
            boolean alreadyProcessed) {
        this(orderId, productId, quantity, previousCount, currentCount, alreadyProcessed, 0);
    }
}
