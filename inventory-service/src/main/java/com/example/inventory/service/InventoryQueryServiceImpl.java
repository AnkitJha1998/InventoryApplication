package com.example.inventory.service;

import com.example.inventory.api.InventoryPageResponse;
import com.example.inventory.api.InventoryResponse;
import com.example.inventory.persistence.repository.InventoryDocumentRepository;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InventoryQueryServiceImpl implements InventoryQueryService {
    private final InventoryDocumentRepository repository;

    public InventoryQueryServiceImpl(InventoryDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public InventoryResponse findByProductId(String productId) throws InventoryNotFoundException {
        log.info("[findByProductId][{}] service entry productId={}", Instant.now(), productId);
        return repository.findById(productId)
                .map(InventoryResponse::from)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
    }

    @Override
    public InventoryPageResponse findAll(int page, int size) throws BusinessException {
        log.info("[findAll][{}] service entry page={} size={}", Instant.now(), page, size);
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException("page must be non-negative and size must be between 1 and 100");
        }
        Page<InventoryResponse> result = repository
                .findAll(PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("updatedAt"),
                                Sort.Order.asc("productId"))))
                .map(InventoryResponse::from);
        return new InventoryPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
