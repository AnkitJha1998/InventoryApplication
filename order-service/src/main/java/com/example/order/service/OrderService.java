package com.example.order.service;

import com.example.order.api.OrderRequest;
import com.example.order.api.OrderResponse;

public interface OrderService {
  OrderResponse place(OrderRequest request);

  default OrderResponse place(OrderRequest request, String authorization) {
    return place(request);
  }
}
