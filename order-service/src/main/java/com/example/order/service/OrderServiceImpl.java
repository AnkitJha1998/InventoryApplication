package com.example.order.service;

import com.example.order.api.OrderRequest;
import com.example.order.api.OrderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class OrderServiceImpl implements OrderService {
    private final RestClient client;

    public OrderServiceImpl(
            RestClient.Builder builder,
            @Value("${inventory-service.base-url:http://localhost:8081}") String inventoryUrl) {
        this.client = builder.baseUrl(inventoryUrl).build();
    }

    @Override
    public OrderResponse place(OrderRequest request)
            throws OrderBusinessException, InventoryDownstreamException {
        return place(request, null);
    }

    @Override
    public OrderResponse place(OrderRequest request, String authorization)
            throws OrderBusinessException, InventoryDownstreamException {
        try {
            var requestSpec = client.post().uri("/inventory/reduce");
            if (authorization != null && !authorization.isBlank()) {
                requestSpec = requestSpec.header("Authorization", authorization);
            }
            ResponseEntity<OrderResponse> response = requestSpec
                    .body(request)
                    .retrieve()
                    .toEntity(OrderResponse.class);
            if (response.getStatusCode().is4xxClientError()) {
                throw new OrderBusinessException(
                        "inventory request rejected: " + response.getStatusCode());
            }
            if (response.getStatusCode().is5xxServerError()) {
                throw new InventoryDownstreamException("inventory service unavailable");
            }
            if (response.getBody() == null) {
                throw new InventoryDownstreamException("inventory service returned an empty response");
            }
            return response.getBody();
        } catch (OrderBusinessException | InventoryDownstreamException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw new OrderBusinessException(
                        "inventory request rejected: " + exception.getStatusCode());
            }
            throw new InventoryDownstreamException("inventory service unavailable");
        } catch (RestClientException exception) {
            throw new InventoryDownstreamException("inventory service unavailable");
        }
    }
}
