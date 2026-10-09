package com.example.notification.service;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;
import org.springframework.stereotype.Component;

@Component
public class InventoryQueryClient {
    private final RestClient client;

    public InventoryQueryClient(
            RestClient.Builder builder,
            @Value("${inventory.query.base-url:http://localhost:8081}") String inventoryBaseUrl) {
        this.client = builder.baseUrl(inventoryBaseUrl).build();
    }

    public InventoryProduct query(String productId, String authorization)
            throws ProductNotFoundException, DownstreamQueryException {
        try {
            InventoryProduct response = client.get()
                    .uri("/inventory/{productId}", productId)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(InventoryProduct.class);
            if (response == null) {
                throw new DownstreamQueryException("inventory query returned no product", null);
            }
            return response;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ProductNotFoundException(productId);
            }
            throw new DownstreamQueryException("inventory query failed", exception);
        } catch (RestClientException exception) {
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
