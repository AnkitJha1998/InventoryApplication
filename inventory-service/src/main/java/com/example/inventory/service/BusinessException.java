package com.example.inventory.service;

public class BusinessException extends RuntimeException {
  public BusinessException(String message) { super(message); }
}
