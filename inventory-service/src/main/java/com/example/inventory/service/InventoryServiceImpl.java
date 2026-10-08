package com.example.inventory.service;

import com.example.inventory.api.ReductionRequest;
import com.example.inventory.api.ReductionResponse;
import com.example.inventory.persistence.Inventory;
import com.example.inventory.persistence.InventoryOperation;
import com.example.inventory.persistence.InventoryOperationRepository;
import com.example.inventory.persistence.InventoryRepository;
import com.example.inventory.persistence.OutboxEvent;
import com.example.inventory.persistence.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryOperationRepository operationRepository;
    private final OutboxEventRepository outboxRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository,
                                InventoryOperationRepository operationRepository,
                                OutboxEventRepository outboxRepository) {
        this.inventoryRepository = inventoryRepository;
        this.operationRepository = operationRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    @Transactional
    public ReductionResponse reduce(ReductionRequest request) {
        validate(request);

        var existing = operationRepository.findByOrderIdAndProductId(request.orderId(), request.productId());
        if (existing.isPresent()) {
            return existingResponse(request, existing.get());
        }

        return reduceNew(request);
    }

    private ReductionResponse reduceNew(ReductionRequest request) {
        int updated = inventoryRepository.reduceAvailableCount(request.productId(), request.quantity());
        if (updated == 0) {
            boolean exists = inventoryRepository.existsById(request.productId());
            throw new BusinessException(exists ? "insufficient stock" : "inventory item not found");
        }

        Inventory inventory = inventoryRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException("inventory item not found"));
        int current = inventory.getAvailableCount();
        int previous = current + request.quantity();

        operationRepository.save(new InventoryOperation(UUID.randomUUID(), request.orderId(), request.productId(),
                request.quantity(), previous, current));
        saveOutbox(request.productId(), previous, current, "InventoryUpdate");
        if (previous == 0 && current > 0) {
            saveOutbox(request.productId(), previous, current, "NotificationTriggers");
        }
        return new ReductionResponse(request.orderId(), request.productId(), request.quantity(), previous, current,
                false);
    }

    private ReductionResponse existingResponse(ReductionRequest request, InventoryOperation existing) {
        if (existing.getQuantity() != request.quantity()) {
            throw new BusinessException("idempotency conflict: quantity differs for orderId/productId");
        }
        return new ReductionResponse(request.orderId(), request.productId(), existing.getQuantity(),
                existing.getPreviousCount(), existing.getCurrentCount(), true);
    }

    private void saveOutbox(String productId, int previous, int current, String topic) {
        outboxRepository.save(new OutboxEvent(UUID.randomUUID(), productId, previous, current, topic));
    }

    private void validate(ReductionRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new BusinessException("quantity must be greater than zero");
        }
        if (blank(request.orderId()) || blank(request.productId())) {
            throw new BusinessException("orderId and productId are required");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
