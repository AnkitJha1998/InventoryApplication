package com.example.inventory.service;

import com.example.inventory.api.ReductionRequest;
import com.example.inventory.api.ReductionResponse;
import com.example.inventory.persistence.entity.Inventory;
import com.example.inventory.persistence.entity.InventoryOperation;
import com.example.inventory.persistence.repository.InventoryOperationRepository;
import com.example.inventory.persistence.repository.InventoryRepository;
import com.example.inventory.persistence.entity.OutboxEvent;
import com.example.inventory.persistence.repository.OutboxEventRepository;
import com.example.inventory.persistence.entity.OutboxStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryOperationRepository operationRepository;
    private final OutboxEventRepository outboxRepository;

    public InventoryServiceImpl(
            InventoryRepository inventoryRepository,
            InventoryOperationRepository operationRepository,
            OutboxEventRepository outboxRepository) {
        this.inventoryRepository = inventoryRepository;
        this.operationRepository = operationRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public ReductionResponse reduce(ReductionRequest request) throws BusinessException {
        validate(request);

        var existing = operationRepository.findByOrderIdAndProductId(request.orderId(), request.productId());
        if (existing.isPresent()) {
            return existingResponse(request, existing.get());
        }

        return reduceNew(request);
    }

    private ReductionResponse reduceNew(ReductionRequest request) throws BusinessException {
        int updated = inventoryRepository.reduceAvailableCount(request.productId(), request.quantity());
        if (updated == 0) {
            boolean exists = inventoryRepository.existsById(request.productId());
            throw new BusinessException(exists ? "insufficient stock" : "inventory item not found");
        }

        Inventory inventory = inventoryRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException("inventory item not found"));
        int current = inventory.getAvailableCount();
        int previous = current + request.quantity();

        operationRepository.save(
                InventoryOperation.builder()
                        .operationId(UUID.randomUUID())
                        .orderId(request.orderId())
                        .productId(request.productId())
                        .quantity(request.quantity())
                        .previousCount(previous)
                        .currentCount(current)
                        .createdAt(Instant.now())
                        .build());
        saveOutbox(inventory, previous, current, "InventoryUpdate");
        if (previous == 0 && current > 0) {
            saveOutbox(inventory, previous, current, "NotificationTriggers");
        }
        return new ReductionResponse(request.orderId(), request.productId(), request.quantity(), previous, current, false);
    }

    private ReductionResponse existingResponse(ReductionRequest request, InventoryOperation existing)
            throws BusinessException {
        if (existing.getQuantity() != request.quantity()) {
            throw new BusinessException("idempotency conflict: quantity differs for orderId/productId");
        }
        return new ReductionResponse(
                request.orderId(),
                request.productId(),
                existing.getQuantity(),
                existing.getPreviousCount(),
                existing.getCurrentCount(),
                true);
    }

    private void saveOutbox(Inventory inventory, int previous, int current, String topic) {
        outboxRepository.save(
                OutboxEvent.builder()
                        .eventId(UUID.randomUUID())
                        .productId(inventory.getProductId())
                        .productName(inventory.getProductName())
                        .productType(inventory.getProductType())
                        .previousCount(previous)
                        .currentCount(current)
                        .topic(topic)
                        .status(OutboxStatus.PENDING)
                        .retryCount(0)
                        .createdAt(Instant.now())
                        .build());
    }

    private void validate(ReductionRequest request) throws BusinessException {
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
