package com.example.inventory.service;

import com.example.inventory.api.IncreaseRequest;
import com.example.inventory.api.IncreaseResponse;
import com.example.inventory.api.ReductionRequest;
import com.example.inventory.api.ReductionResponse;

public interface InventoryService {
    ReductionResponse reduce(ReductionRequest request) throws BusinessException;

    IncreaseResponse increase(IncreaseRequest request)
            throws BusinessException, InventoryNotFoundException;
}
