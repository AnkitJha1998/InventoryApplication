package com.example.notification.service;

import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InventoryQueryClient {
    private final RestClient client;

    public InventoryQueryClient(
            RestClient.Builder builder,
            @Value("${inventory.query.base-url:http://localhost:8081}") String inventoryBaseUrl) {
        this.client = builder.baseUrl(inventoryBaseUrl).build();
    }

    public InventoryProduct query(String productId, String authorization)
            throws ProductNotFoundException, DownstreamQueryException {
        log.info("[query][{}] REST client entry productId={}", Instant.now(), productId);
        try {
            InventoryProduct response = client.get()
                    .uri("/inventory/{productId}", productId)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(InventoryProduct.class);
            if (response == null) {
                throw new DownstreamQueryException("inventory query returned no product", null);
            }
            log.info("[query][{}] inventory query completed productId={} quantity={}",
                    Instant.now(), productId, response.quantity());
            return response;
        } catch (RestClientResponseException exception) {
            log.error("[query][{}] inventory query response failed productId={} status={}",
                    Instant.now(), productId, exception.getStatusCode(), exception);
            if (exception.getStatusCode().value() == 404) {
                throw new ProductNotFoundException(productId);
            }
            throw new DownstreamQueryException("inventory query failed", exception);
        } catch (RestClientException exception) {
            log.error("[query][{}] inventory query failed productId={}", Instant.now(), productId,
                    exception);
            throw new DownstreamQueryException("inventory query failed", exception);
        }
    }

    public record InventoryProduct(
            String productId,
            String productName,
            String productType,
            Integer availableCount,
            java.time.Instant updatedAt,
            Integer availableQuantity,
            Boolean available,
            Long version) {
        public int quantity() {
            return availableQuantity != null
                    ? availableQuantity
                    : availableCount == null ? 0 : availableCount;
        }
    }
}
