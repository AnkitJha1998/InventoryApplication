package com.example.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.notification.api.BackInStockRequestBody;
import com.example.notification.persistence.entity.NotificationStatus;
import com.example.notification.persistence.repository.BackInStockRequestRepository;
import com.example.notification.persistence.repository.NotificationRepository;
import com.example.notification.persistence.repository.ProcessedEventRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

class NotificationServiceImplTest {
  @Test
  void requestRequiresExactlyZeroQuantity() throws Exception {
    BackInStockRequestRepository requests =
        org.mockito.Mockito.mock(BackInStockRequestRepository.class);
    NotificationRepository notifications = org.mockito.Mockito.mock(NotificationRepository.class);
    ProcessedEventRepository events = org.mockito.Mockito.mock(ProcessedEventRepository.class);
    InventoryQueryClient inventory = org.mockito.Mockito.mock(InventoryQueryClient.class);
    NotificationServiceImpl service =
        new NotificationServiceImpl(requests, notifications, events, inventory);
    UUID customerId = UUID.randomUUID();
    when(inventory.query("P1", "Bearer token"))
        .thenReturn(
            new InventoryQueryClient.InventoryProduct(
                "P1", "Demo", "STANDARD", 0, null, -1, false, 1L));

    assertThrows(
        NotificationException.class,
        () ->
            service.requestBackInStock(
                customerId, new BackInStockRequestBody("P1"), "Bearer token"));
    verify(requests, never()).save(any());
  }

  @Test
  void bulkReadOnlyChangesCreatedNotificationsForCustomer() throws Exception {
    BackInStockRequestRepository requests =
        org.mockito.Mockito.mock(BackInStockRequestRepository.class);
    NotificationRepository notifications = org.mockito.Mockito.mock(NotificationRepository.class);
    ProcessedEventRepository events = org.mockito.Mockito.mock(ProcessedEventRepository.class);
    InventoryQueryClient inventory = org.mockito.Mockito.mock(InventoryQueryClient.class);
    NotificationServiceImpl service =
        new NotificationServiceImpl(requests, notifications, events, inventory);
    UUID customerId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();
    when(notifications.markAsRead(
            org.mockito.ArgumentMatchers.eq(customerId),
            org.mockito.ArgumentMatchers.eq(List.of(notificationId)),
            any(),
            org.mockito.ArgumentMatchers.eq(NotificationStatus.CREATED),
            org.mockito.ArgumentMatchers.eq(NotificationStatus.NOTIFIED)))
        .thenReturn(1);

    assertEquals(1, service.markAsRead(customerId, List.of(notificationId)));
    verify(notifications)
        .markAsRead(
            org.mockito.ArgumentMatchers.eq(customerId),
            org.mockito.ArgumentMatchers.eq(List.of(notificationId)),
            any(),
            org.mockito.ArgumentMatchers.eq(NotificationStatus.CREATED),
            org.mockito.ArgumentMatchers.eq(NotificationStatus.NOTIFIED));
  }

  @Test
  void paginationRejectsInvalidSizeAndUsesUnreadStatus() throws Exception {
    BackInStockRequestRepository requests =
        org.mockito.Mockito.mock(BackInStockRequestRepository.class);
    NotificationRepository notifications = org.mockito.Mockito.mock(NotificationRepository.class);
    ProcessedEventRepository events = org.mockito.Mockito.mock(ProcessedEventRepository.class);
    InventoryQueryClient inventory = org.mockito.Mockito.mock(InventoryQueryClient.class);
    NotificationServiceImpl service =
        new NotificationServiceImpl(requests, notifications, events, inventory);
    UUID customerId = UUID.randomUUID();
    when(notifications.countByCustomerIdAndStatus(customerId, NotificationStatus.CREATED))
        .thenReturn(2L);
    assertEquals(2L, service.unreadCount(customerId));
    assertThrows(NotificationException.class, () -> service.findNotifications(customerId, 0, 101));
    when(notifications.findByCustomerId(any(), any())).thenReturn(new PageImpl<>(List.of()));
    assertEquals(0, service.findNotifications(customerId, 0, 20).getTotalElements());
  }
}
