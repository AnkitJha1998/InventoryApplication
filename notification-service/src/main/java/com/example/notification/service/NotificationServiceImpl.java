package com.example.notification.service;

import com.example.notification.api.BackInStockRequestBody;
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
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {
  private final BackInStockRequestRepository requestRepository;
  private final NotificationRepository notificationRepository;
  private final ProcessedEventRepository processedEventRepository;
  private final InventoryQueryClient inventoryQueryClient;

  public NotificationServiceImpl(
      BackInStockRequestRepository requestRepository,
      NotificationRepository notificationRepository,
      ProcessedEventRepository processedEventRepository,
      InventoryQueryClient inventoryQueryClient) {
    this.requestRepository = requestRepository;
    this.notificationRepository = notificationRepository;
    this.processedEventRepository = processedEventRepository;
    this.inventoryQueryClient = inventoryQueryClient;
  }

  @Override
  @Transactional(rollbackFor = NotificationException.class)
  public BackInStockRequest requestBackInStock(
      UUID customerId, BackInStockRequestBody body, String authorization)
      throws NotificationException {
    log.info("[requestBackInStock][{}] service entry customerId={} productId={}", Instant.now(),
        customerId, body == null ? "unknown" : body.productId());
    if (customerId == null
        || body == null
        || body.productId() == null
        || body.productId().isBlank()) {
      throw new NotificationException("productId is required");
    }
    InventoryQueryClient.InventoryProduct product =
        inventoryQueryClient.query(body.productId(), authorization);
    if (product.quantity() != 0) {
      throw new NotificationException("product is already in stock");
    }
    if (requestRepository.existsByCustomerIdAndProductIdAndStatus(
        customerId, body.productId(), BackInStockStatus.ACTIVE)) {
      throw new NotificationException("an active request already exists");
    }
    Instant now = Instant.now();
    BackInStockRequest request =
        BackInStockRequest.builder()
            .requestId(UUID.randomUUID())
            .customerId(customerId)
            .productId(body.productId())
            .status(BackInStockStatus.ACTIVE)
            .createdAt(now)
            .updatedAt(now)
            .build();
    try {
      BackInStockRequest saved = requestRepository.save(request);
      log.info("[requestBackInStock][{}] notification request created requestId={} customerId={} productId={}",
          Instant.now(), saved == null ? "unknown" : saved.getRequestId(), customerId, body.productId());
      return saved;
    } catch (DataIntegrityViolationException exception) {
      log.error("[requestBackInStock][{}] notification request creation failed customerId={} productId={}",
          Instant.now(), customerId, body.productId(), exception);
      throw new NotificationException("an active request already exists", exception);
    }
  }

  @Override
  @Transactional(rollbackFor = NotificationException.class)
  public void process(NotificationTrigger trigger) throws NotificationException {
    log.info("[process][{}] service entry eventId={} productId={}", Instant.now(),
        trigger == null ? "unknown" : trigger.eventId(),
        trigger == null ? "unknown" : trigger.productId());
    validateTrigger(trigger);
    if (processedEventRepository.existsById(trigger.eventId())) {
      log.info("[process][{}] notification trigger already processed eventId={}",
          Instant.now(), trigger.eventId());
      return;
    }
    List<BackInStockRequest> requests =
        requestRepository.findByProductIdAndStatus(trigger.productId(), BackInStockStatus.ACTIVE);
    if (requests.isEmpty()) {
      log.info("[process][{}] no active notification requests eventId={} productId={}",
          Instant.now(), trigger.eventId(), trigger.productId());
      return;
    }
    Instant now = Instant.now();
    String message = trigger.productName() + " is now available";
    List<Notification> notifications =
        requests
            .stream()
            .map(
                request -> {
                  request.setStatus(BackInStockStatus.COMPLETED);
                  request.setUpdatedAt(now);
                  return Notification.builder()
                      .notificationId(UUID.randomUUID())
                      .customerId(request.getCustomerId())
                      .productId(request.getProductId())
                      .requestId(request.getRequestId())
                      .eventId(trigger.eventId())
                      .message(message)
                      .status(NotificationStatus.CREATED)
                      .createdAt(now)
                      .build();
                })
            .toList();

    notificationRepository.saveAll(notifications);
    requestRepository.saveAll(requests);
    processedEventRepository.save(
        ProcessedEvent.builder().eventId(trigger.eventId()).processedAt(now).build());
    log.info("[process][{}] notification requests completed and notifications created eventId={} productId={} count={}",
        Instant.now(), trigger.eventId(), trigger.productId(), notifications.size());
  }

  @Override
  @Transactional(readOnly = true)
  public long unreadCount(UUID customerId) {
    log.info("[unreadCount][{}] service entry customerId={}", Instant.now(), customerId);
    return notificationRepository.countByCustomerIdAndStatus(
        customerId, NotificationStatus.CREATED);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Notification> findNotifications(UUID customerId, int page, int size)
      throws NotificationException {
    log.info("[findNotifications][{}] service entry customerId={} page={} size={}",
        Instant.now(), customerId, page, size);
    if (page < 0) {
      throw new NotificationException("page must be zero or greater");
    }
    if (size < 1 || size > 100) {
      throw new NotificationException("size must be between 1 and 100");
    }
    Sort sort =
        Sort.by(Sort.Order.desc("createdAt")).and(Sort.by(Sort.Order.asc("notificationId")));
    return notificationRepository.findByCustomerId(customerId, PageRequest.of(page, size, sort));
  }

  @Override
  @Transactional(rollbackFor = NotificationException.class)
  public int markAsRead(UUID customerId, List<UUID> notificationIds) throws NotificationException {
    log.info("[markAsRead][{}] service entry customerId={} notificationCount={}",
        Instant.now(), customerId, notificationIds == null ? 0 : notificationIds.size());
    if (notificationIds == null
        || notificationIds.isEmpty()
        || notificationIds.stream().anyMatch(id -> id == null)) {
      throw new NotificationException("notificationIds must not be empty");
    }
    int updatedCount = notificationRepository.markAsRead(
        customerId,
        notificationIds,
        Instant.now(),
        NotificationStatus.CREATED,
        NotificationStatus.NOTIFIED);
    log.info("[markAsRead][{}] notification read updates customerId={} count={}",
        Instant.now(), customerId, updatedCount);
    return updatedCount;
  }

  private void validateTrigger(NotificationTrigger trigger) throws NotificationException {
    if (trigger == null
        || trigger.eventId() == null
        || trigger.productId() == null
        || trigger.productId().isBlank()
        || trigger.productName() == null
        || trigger.productName().isBlank()
        || trigger.occurredAt() == null
        || trigger.topic() == null
        || !"NotificationTrigger".equals(trigger.topic())) {
      throw new NotificationException("invalid NotificationTrigger payload");
    }
  }
}
