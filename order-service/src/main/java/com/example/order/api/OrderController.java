package com.example.order.api;

import com.example.order.service.InventoryDownstreamException;
import com.example.order.service.OrderBusinessException;
import com.example.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderController {
  private final OrderService service;

  public OrderController(OrderService service) {
    this.service = service;
  }

  @PostMapping("/orders")
  public ResponseEntity<OrderResponse> create(
      @Valid @RequestBody OrderRequest request,
      @RequestHeader(value = "Authorization", required = false) String authorization)
      throws OrderBusinessException, InventoryDownstreamException {
    return ResponseEntity.ok(service.place(request, authorization));
  }
}
