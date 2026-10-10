package com.example.inventory.service;

public class InventoryNotFoundException extends Exception {
  public InventoryNotFoundException(String productId) {
    super("inventory item not found: " + productId);
  }
}
