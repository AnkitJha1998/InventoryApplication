package com.example.order.service;

import com.example.order.api.OrderRequest;
import com.example.order.api.OrderResponse;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
@Slf4j
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
        log.info("[place][{}] service entry orderId={} productId={}", Instant.now(),
                request == null ? "unknown" : request.orderId(),
                request == null ? "unknown" : request.productId());
        try {
            var requestSpec = client.post().uri("/inventory/reduce");
            if (authorization != null && !authorization.isBlank()) {
                requestSpec = requestSpec.header("Authorization", authorization);
            }
            log.info("[place][{}] delegating order to inventory orderId={} productId={}",
                    Instant.now(), request == null ? "unknown" : request.orderId(),
                    request == null ? "unknown" : request.productId());
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
            log.info("[place][{}] order delegation completed orderId={} productId={}",
                    Instant.now(), response.getBody().orderId(), response.getBody().productId());
            return response.getBody();
        } catch (OrderBusinessException | InventoryDownstreamException exception) {
            log.error("[place][{}] order delegation failed orderId={} productId={}", Instant.now(),
                    request == null ? "unknown" : request.orderId(),
                    request == null ? "unknown" : request.productId(), exception);
            throw exception;
        } catch (RestClientResponseException exception) {
            log.error("[place][{}] inventory response failed orderId={} productId={} status={}",
                    Instant.now(), request == null ? "unknown" : request.orderId(),
                    request == null ? "unknown" : request.productId(), exception.getStatusCode(),
                    exception);
            if (exception.getStatusCode().is4xxClientError()) {
                throw new OrderBusinessException(
                        "inventory request rejected: " + exception.getStatusCode());
            }
            throw new InventoryDownstreamException("inventory service unavailable");
        } catch (RestClientException exception) {
            log.error("[place][{}] inventory request failed orderId={} productId={}", Instant.now(),
                    request == null ? "unknown" : request.orderId(),
                    request == null ? "unknown" : request.productId(), exception);
            throw new InventoryDownstreamException("inventory service unavailable");
        }
    }
}
