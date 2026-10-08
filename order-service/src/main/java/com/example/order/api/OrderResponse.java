package com.example.order.api;

public record OrderResponse(String orderId, String productId, int quantity,
                            int previousCount, int currentCount, boolean alreadyProcessed) {}
