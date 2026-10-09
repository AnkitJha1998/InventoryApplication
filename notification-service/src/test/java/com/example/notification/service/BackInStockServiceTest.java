package com.example.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.notification.api.BackInStockRequestBody;
import com.example.notification.persistence.entity.BackInStockStatus;
import com.example.notification.persistence.repository.BackInStockRequestRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BackInStockServiceTest {
  @Test
  void zeroStockCreatesActiveRequest() throws Exception {
    BackInStockRequestRepository requests = mock(BackInStockRequestRepository.class);
    InventoryQueryClient inventory = mock(InventoryQueryClient.class);
    BackInStockService service = new BackInStockService(requests, inventory);
    UUID customerId = UUID.randomUUID();
    when(inventory.query("P1", "Bearer token"))
        .thenReturn(
            new InventoryQueryClient.InventoryProduct(
                "P1", "Demo", "STANDARD", 0, null, 0, false, 1L));
    when(requests.existsByCustomerIdAndProductIdAndStatus(
            customerId, "P1", BackInStockStatus.ACTIVE))
        .thenReturn(false);
    when(requests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.request(customerId, new BackInStockRequestBody("P1"), "Bearer token");

    assertEquals("P1", result.getProductId());
    assertEquals(BackInStockStatus.ACTIVE, result.getStatus());
    verify(requests).save(any());
  }

  @Test
  void availableProductIsRejected() throws Exception {
    BackInStockRequestRepository requests = mock(BackInStockRequestRepository.class);
    InventoryQueryClient inventory = mock(InventoryQueryClient.class);
    BackInStockService service = new BackInStockService(requests, inventory);
    when(inventory.query("P1", "Bearer token"))
        .thenReturn(
            new InventoryQueryClient.InventoryProduct(
                "P1", "Demo", "STANDARD", 2, null, 2, true, 1L));

    assertThrows(
        NotificationException.class,
        () -> service.request(UUID.randomUUID(), new BackInStockRequestBody("P1"), "Bearer token"));
    verify(requests, never()).save(any());
  }

  @Test
  void duplicateActiveRequestIsRejected() throws Exception {
    BackInStockRequestRepository requests = mock(BackInStockRequestRepository.class);
    InventoryQueryClient inventory = mock(InventoryQueryClient.class);
    BackInStockService service = new BackInStockService(requests, inventory);
    UUID customerId = UUID.randomUUID();
    when(inventory.query("P1", "Bearer token"))
        .thenReturn(
            new InventoryQueryClient.InventoryProduct(
                "P1", "Demo", "STANDARD", 0, null, 0, false, 1L));
    when(requests.existsByCustomerIdAndProductIdAndStatus(
            customerId, "P1", BackInStockStatus.ACTIVE))
        .thenReturn(true);

    assertThrows(
        NotificationException.class,
        () -> service.request(customerId, new BackInStockRequestBody("P1"), "Bearer token"));
    verify(requests, never()).save(any());
  }

  @Test
  void downstreamQueryFailurePropagates() throws Exception {
    BackInStockRequestRepository requests = mock(BackInStockRequestRepository.class);
    InventoryQueryClient inventory = mock(InventoryQueryClient.class);
    BackInStockService service = new BackInStockService(requests, inventory);
    when(inventory.query("P1", "Bearer token"))
        .thenThrow(new DownstreamQueryException("inventory query failed", null));

    assertThrows(
        DownstreamQueryException.class,
        () -> service.request(UUID.randomUUID(), new BackInStockRequestBody("P1"), "Bearer token"));
    verify(requests, never()).save(any());
  }
}
