package com.example.notification.consumer;

import com.example.notification.service.NotificationException;
import com.example.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationTriggerConsumer {
  private final ObjectMapper objectMapper;
  private final NotificationService notificationService;

  @Autowired
  public NotificationTriggerConsumer(
      ObjectMapper objectMapper, NotificationService notificationService) {
    this.objectMapper = objectMapper;
    this.objectMapper.findAndRegisterModules();
    this.notificationService = notificationService;
  }

  @KafkaListener(
      topics = "NotificationTrigger",
      groupId = "${notification.consumer.group:notification-consumer}")
  public void onMessage(String payload, Acknowledgment acknowledgment) {
    log.info("[onMessage][{}] consumer entry", Instant.now());
    final NotificationTrigger trigger;
    try {
      trigger = objectMapper.readValue(payload, NotificationTrigger.class);
    } catch (Exception exception) {
      log.error("[onMessage][{}] malformed NotificationTrigger payload acknowledged",
          Instant.now(), exception);
      acknowledgment.acknowledge();
      return;
    }
    if (trigger == null
        || trigger.eventId() == null
        || trigger.productId() == null
        || trigger.productId().isBlank()
        || trigger.productName() == null
        || trigger.productName().isBlank()
        || trigger.occurredAt() == null
        || trigger.topic() == null
        || !"NotificationTrigger".equals(trigger.topic())) {
      log.error("[onMessage][{}] malformed NotificationTrigger payload acknowledged",
          Instant.now());
      acknowledgment.acknowledge();
      return;
    }
    try {
      notificationService.process(trigger);
      acknowledgment.acknowledge();
      log.info("[onMessage][{}] NotificationTrigger processed eventId={} productId={}",
          Instant.now(), trigger.eventId(), trigger.productId());
    } catch (NotificationException | RuntimeException exception) {
      log.error("[onMessage][{}] NotificationTrigger was not processed eventId={} productId={}",
          Instant.now(), trigger.eventId(), trigger.productId(), exception);
    }
  }
}
