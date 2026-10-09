package com.example.notification.service;

import com.example.notification.consumer.NotificationTrigger;
import com.example.notification.persistence.entity.BackInStockRequest;
import com.example.notification.persistence.entity.BackInStockStatus;
import com.example.notification.persistence.entity.Notification;
import com.example.notification.persistence.entity.NotificationStatus;
import com.example.notification.persistence.entity.ProcessedEvent;
import com.example.notification.persistence.repository.BackInStockRequestRepository;
import com.example.notification.persistence.repository.NotificationRepository;
import com.example.notification.persistence.repository.ProcessedEventRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationTriggerService {
  private final BackInStockRequestRepository requestRepository;
  private final NotificationRepository notificationRepository;
  private final ProcessedEventRepository processedEventRepository;

  public NotificationTriggerService(
      BackInStockRequestRepository requestRepository,
      NotificationRepository notificationRepository,
      ProcessedEventRepository processedEventRepository) {
    this.requestRepository = requestRepository;
    this.notificationRepository = notificationRepository;
    this.processedEventRepository = processedEventRepository;
  }

  @Transactional(rollbackFor = NotificationException.class)
  public void process(NotificationTrigger trigger) throws NotificationException {
    if (trigger == null
        || trigger.eventId() == null
        || trigger.productId() == null
        || trigger.productName() == null
        || trigger.occurredAt() == null) {
      throw new NotificationException("invalid NotificationTrigger payload");
    }
    if (processedEventRepository.existsById(trigger.eventId())) {
      return;
    }
    List<BackInStockRequest> requests =
        requestRepository.findByProductIdAndStatus(trigger.productId(), BackInStockStatus.ACTIVE);
    if (requests.isEmpty()) {
      return;
    }
    Instant now = Instant.now();
    String message = trigger.productName() + " is now available";
    for (BackInStockRequest request : requests) {
      request.setStatus(BackInStockStatus.COMPLETED);
      request.setUpdatedAt(now);
      requestRepository.save(request);
      notificationRepository.save(
          Notification.builder()
              .notificationId(java.util.UUID.randomUUID())
              .customerId(request.getCustomerId())
              .productId(request.getProductId())
              .requestId(request.getRequestId())
              .eventId(trigger.eventId())
              .message(message)
              .status(NotificationStatus.PREPARED)
              .createdAt(now)
              .build());
    }
    processedEventRepository.save(
        ProcessedEvent.builder().eventId(trigger.eventId()).processedAt(now).build());
  }
}
