package com.example.inventory.service;

import com.example.inventory.api.InventoryPageResponse;
import com.example.inventory.api.InventoryResponse;

public interface InventoryQueryService {
  InventoryResponse findByProductId(String productId) throws InventoryNotFoundException;

  InventoryPageResponse findAll(int page, int size) throws BusinessException;
}
