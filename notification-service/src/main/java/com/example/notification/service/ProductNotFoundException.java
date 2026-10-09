package com.example.notification.service;

public class ProductNotFoundException extends NotificationException {
  public ProductNotFoundException(String productId) {
    super("product not found: " + productId);
  }
}
