package com.example.order.service;

import com.example.order.api.OrderRequest;
import com.example.order.api.OrderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OrderServiceImpl implements OrderService {
  private final RestClient client;

  public OrderServiceImpl(
      RestClient.Builder builder,
      @Value("${inventory-service.base-url:http://localhost:8081}") String inventoryUrl) {
    this.client = builder.baseUrl(inventoryUrl).build();
  }

  @Override
  public OrderResponse place(OrderRequest request) {
    return place(request, null);
  }

  @Override
  public OrderResponse place(OrderRequest request, String authorization) {
    try {
      var requestSpec = client.post().uri("/inventory/reduce");
      if (authorization != null && !authorization.isBlank()) {
        requestSpec = requestSpec.header("Authorization", authorization);
      }
      return requestSpec
          .body(request)
          .retrieve()
          .onStatus(
              HttpStatusCode::is4xxClientError,
              (req, res) -> {
                throw new OrderBusinessException(
                    "inventory request rejected: " + res.getStatusCode());
              })
          .onStatus(
              HttpStatusCode::is5xxServerError,
              (req, res) -> {
                throw new InventoryDownstreamException("inventory service unavailable");
              })
          .body(OrderResponse.class);
    } catch (OrderBusinessException | InventoryDownstreamException exception) {
      throw exception;
    } catch (RestClientException exception) {
      throw new InventoryDownstreamException("inventory service unavailable");
    }
  }
}
