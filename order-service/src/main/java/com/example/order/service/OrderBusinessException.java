package com.example.order.service;

public class OrderBusinessException extends RuntimeException {
    public OrderBusinessException(String message) {
        super(message);
    }
}
