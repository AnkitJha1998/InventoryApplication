package com.example.inventory.api;

import com.example.inventory.service.BusinessException;
import com.example.inventory.service.InventoryNotFoundException;
import com.example.inventory.service.InventoryQueryService;
import com.example.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
    private final InventoryService service;
    private final InventoryQueryService queryService;

    public InventoryController(InventoryService service) {
        this(service, null);
    }

    @Autowired
    public InventoryController(
            InventoryService service,
            @Nullable InventoryQueryService queryService) {
        this.service = service;
        this.queryService = queryService;
    }

    @PostMapping("/create")
    public ResponseEntity<CreateResponse> create(@Valid @RequestBody CreateRequest request)
            throws BusinessException {
        return ResponseEntity.ok(service.create(request));
    }

    @PostMapping("/reduce")
    public ResponseEntity<ReductionResponse> reduce(@Valid @RequestBody ReductionRequest request)
            throws BusinessException {
        return ResponseEntity.ok(service.reduce(request));
    }

    @PostMapping("/increase")
    public ResponseEntity<IncreaseResponse> increase(@Valid @RequestBody IncreaseRequest request)
            throws BusinessException, InventoryNotFoundException {
        return ResponseEntity.ok(service.increase(request));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> findByProductId(@PathVariable String productId)
            throws InventoryNotFoundException {
        return ResponseEntity.ok(queryService.findByProductId(productId));
    }

    @GetMapping
    public ResponseEntity<InventoryPageResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) throws BusinessException {
        return ResponseEntity.ok(queryService.findAll(page, size));
    }
}
