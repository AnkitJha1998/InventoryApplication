package com.example.inventory.service;

import com.example.inventory.api.CreateRequest;
import com.example.inventory.api.CreateResponse;
import com.example.inventory.api.IncreaseRequest;
import com.example.inventory.api.IncreaseResponse;
import com.example.inventory.api.ReductionRequest;
import com.example.inventory.api.ReductionResponse;
import com.example.inventory.persistence.entity.Inventory;
import com.example.inventory.persistence.entity.InventoryOperation;
import com.example.inventory.persistence.entity.OutboxEvent;
import com.example.inventory.persistence.entity.OutboxStatus;
import com.example.inventory.persistence.repository.InventoryOperationRepository;
import com.example.inventory.persistence.repository.InventoryRepository;
import com.example.inventory.persistence.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {
    private static final String INVENTORY_UPDATE_TOPIC = "InventoryUpdate";
    private static final String NOTIFICATION_TRIGGER_TOPIC = "NotificationTrigger";

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
    public CreateResponse create(CreateRequest request) throws BusinessException {
        validateCreation(request);
        if (inventoryRepository.existsById(request.productId())) {
            throw new BusinessException("inventory item already exists");
        }

        Instant occurredAt = Instant.now();
        Inventory inventory = Inventory.builder()
                .productId(request.productId())
                .productName(request.productName())
                .productType(request.productType())
                .availableCount(request.quantity())
                .version(1)
                .updatedAt(occurredAt)
                .build();
        inventoryRepository.save(inventory);
        saveOutbox(inventory, 0, request.quantity(), 1, occurredAt);
        return new CreateResponse(
                inventory.getProductId(),
                inventory.getProductName(),
                inventory.getProductType(),
                inventory.getAvailableCount(),
                inventory.getVersion());
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public ReductionResponse reduce(ReductionRequest request) throws BusinessException {
        validateReduction(request);

        var existing = operationRepository.findByOrderIdAndProductId(
                request.orderId(), request.productId());
        if (existing.isPresent()) {
            return existingResponse(request, existing.get());
        }

        int updated = inventoryRepository.reduceAvailableCount(
                request.productId(), request.quantity());
        if (updated == 0) {
            boolean exists = inventoryRepository.existsById(request.productId());
            throw new BusinessException(exists ? "insufficient stock" : "inventory item not found");
        }

        Inventory inventory = inventoryRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException("inventory item not found"));
        int current = inventory.getAvailableCount();
        int previous = current + request.quantity();
        long version = resultingVersion(inventory);
        Instant occurredAt = Instant.now();

        operationRepository.save(
                InventoryOperation.builder()
                        .operationId(UUID.randomUUID())
                        .orderId(request.orderId())
                        .productId(request.productId())
                        .quantity(request.quantity())
                        .previousCount(previous)
                        .currentCount(current)
                        .version(version)
                        .createdAt(occurredAt)
                        .build());
        saveOutbox(inventory, previous, current, version, occurredAt);
        return new ReductionResponse(
                request.orderId(), request.productId(), request.quantity(), previous, current, false, version);
    }

    @Override
    @Transactional(rollbackFor = {BusinessException.class, InventoryNotFoundException.class})
    public IncreaseResponse increase(IncreaseRequest request)
            throws BusinessException, InventoryNotFoundException {
        validateIncrease(request);

        int updated = inventoryRepository.increaseAvailableCount(
                request.productId(), request.quantity());
        if (updated == 0) {
            throw new InventoryNotFoundException(request.productId());
        }

        Inventory inventory = inventoryRepository.findById(request.productId())
                .orElseThrow(() -> new InventoryNotFoundException(request.productId()));
        long version = resultingVersion(inventory);
        int previous = inventory.getAvailableCount() - request.quantity();
        Instant occurredAt = Instant.now();
        saveOutbox(inventory, previous, inventory.getAvailableCount(), version, occurredAt);
        if (previous == 0 && inventory.getAvailableCount() > 0) {
            saveNotificationTriggerOutbox(inventory, inventory.getAvailableCount(), version, occurredAt);
        }
        return new IncreaseResponse(inventory.getProductId(), inventory.getAvailableCount(), version);
    }

    private ReductionResponse existingResponse(
            ReductionRequest request,
            InventoryOperation existing) throws BusinessException {
        if (existing.getQuantity() != request.quantity()) {
            throw new BusinessException("idempotency conflict: quantity differs for orderId/productId");
        }
        return new ReductionResponse(
                request.orderId(),
                request.productId(),
                existing.getQuantity(),
                existing.getPreviousCount(),
                existing.getCurrentCount(),
                true,
                existing.getVersion());
    }

    private void saveOutbox(
            Inventory inventory,
            int previous,
            int current,
            long version,
            Instant occurredAt) {
        outboxRepository.save(buildOutboxEvent(
                inventory, previous, current, version, occurredAt, INVENTORY_UPDATE_TOPIC));
    }

    private void saveNotificationTriggerOutbox(
            Inventory inventory,
            int current,
            long version,
            Instant occurredAt) {
        outboxRepository.save(buildOutboxEvent(
                inventory, 0, current, version, occurredAt, NOTIFICATION_TRIGGER_TOPIC));
    }

    private OutboxEvent buildOutboxEvent(
            Inventory inventory,
            int previous,
            int current,
            long version,
            Instant occurredAt,
            String topic) {
        return OutboxEvent.builder()
                .eventId(UUID.randomUUID())
                .productId(inventory.getProductId())
                .productName(inventory.getProductName())
                .productType(inventory.getProductType())
                .previousCount(previous)
                .currentCount(current)
                .version(version)
                .occurredAt(occurredAt)
                .topic(topic)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(occurredAt)
                .build();
    }

    private void validateCreation(CreateRequest request) throws BusinessException {
        if (request == null) {
            throw new BusinessException("request is required");
        }
        if (blank(request.productId())
                || blank(request.productName())
                || blank(request.productType())) {
            throw new BusinessException("productId, productName and productType are required");
        }
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new BusinessException("quantity must be greater than zero");
        }
    }

    private void validateReduction(ReductionRequest request) throws BusinessException {
        if (request == null
                || request.quantity() == null
                || request.quantity() <= 0) {
            throw new BusinessException("quantity must be greater than zero");
        }
        if (blank(request.orderId()) || blank(request.productId())) {
            throw new BusinessException("orderId and productId are required");
        }
    }

    private void validateIncrease(IncreaseRequest request) throws BusinessException {
        if (request == null || request.quantity() == null || request.quantity() <= 0) {
            throw new BusinessException("quantity must be greater than zero");
        }
        if (blank(request.productId())) {
            throw new BusinessException("productId is required");
        }
    }

    private long resultingVersion(Inventory inventory) {
        return inventory.getVersion() == 0 ? 1 : inventory.getVersion();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
