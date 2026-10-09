package com.example.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.notification.consumer.NotificationTrigger;
import com.example.notification.persistence.entity.BackInStockRequest;
import com.example.notification.persistence.entity.BackInStockStatus;
import com.example.notification.persistence.repository.BackInStockRequestRepository;
import com.example.notification.persistence.repository.NotificationRepository;
import com.example.notification.persistence.repository.ProcessedEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NotificationTriggerServiceTest {
  @Test
  void idleTriggerDoesNotPersistNotificationOrProcessedEvent() throws Exception {
    BackInStockRequestRepository requests = Mockito.mock(BackInStockRequestRepository.class);
    NotificationRepository notifications = Mockito.mock(NotificationRepository.class);
    ProcessedEventRepository processed = Mockito.mock(ProcessedEventRepository.class);
    NotificationTriggerService service =
        new NotificationTriggerService(requests, notifications, processed);
    UUID eventId = UUID.randomUUID();
    when(processed.existsById(eventId)).thenReturn(false);
    when(requests.findByProductIdAndStatus("P1", BackInStockStatus.ACTIVE)).thenReturn(List.of());

    service.process(
        new NotificationTrigger(
            eventId, "P1", "Demo", "STANDARD", 0, 3, 2, Instant.now(), "NotificationTrigger"));

    verify(notifications, never()).save(any());
    verify(processed, never()).save(any());
  }

  @Test
  void triggerCompletesAllRequestsWithExactMessage() throws Exception {
    BackInStockRequestRepository requests = Mockito.mock(BackInStockRequestRepository.class);
    NotificationRepository notifications = Mockito.mock(NotificationRepository.class);
    ProcessedEventRepository processed = Mockito.mock(ProcessedEventRepository.class);
    NotificationTriggerService service =
        new NotificationTriggerService(requests, notifications, processed);
    UUID eventId = UUID.randomUUID();
    BackInStockRequest first = request();
    BackInStockRequest second = request();
    when(processed.existsById(eventId)).thenReturn(false);
    when(requests.findByProductIdAndStatus("P1", BackInStockStatus.ACTIVE))
        .thenReturn(List.of(first, second));

    service.process(
        new NotificationTrigger(
            eventId, "P1", "Demo", "STANDARD", 0, 3, 2, Instant.now(), "NotificationTrigger"));

    verify(notifications, org.mockito.Mockito.times(2))
        .save(
            org.mockito.ArgumentMatchers.argThat(
                notification -> "Demo is now available".equals(notification.getMessage())));
    verify(processed).save(any());
  }

  private BackInStockRequest request() {
    Instant now = Instant.now();
    return BackInStockRequest.builder()
        .requestId(UUID.randomUUID())
        .customerId(UUID.randomUUID())
        .productId("P1")
        .status(BackInStockStatus.ACTIVE)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
