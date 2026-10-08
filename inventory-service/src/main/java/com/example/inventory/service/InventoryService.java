package com.example.inventory.service;

import com.example.inventory.api.ReductionRequest;
import com.example.inventory.api.ReductionResponse;

public interface InventoryService {
    ReductionResponse reduce(ReductionRequest request);
}
