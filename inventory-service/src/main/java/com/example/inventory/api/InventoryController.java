package com.example.inventory.api;

import com.example.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @PostMapping("/reduce")
    public ResponseEntity<ReductionResponse> reduce(@Valid @RequestBody ReductionRequest request) {
        return ResponseEntity.ok(service.reduce(request));
    }
}
