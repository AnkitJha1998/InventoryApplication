package com.example.notification.consumer;

import com.example.notification.service.NotificationException;
import com.example.notification.service.NotificationTriggerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class NotificationTriggerConsumer {
  private static final Logger LOGGER =
      Logger.getLogger(NotificationTriggerConsumer.class.getName());
  private final ObjectMapper objectMapper;
  private final NotificationTriggerService triggerService;

  public NotificationTriggerConsumer(
      ObjectMapper objectMapper, NotificationTriggerService triggerService) {
    this.objectMapper = objectMapper;
    this.objectMapper.findAndRegisterModules();
    this.triggerService = triggerService;
  }

  @KafkaListener(topics = "NotificationTrigger", groupId = "${notification.consumer.group}")
  public void onMessage(String payload, Acknowledgment acknowledgment) {
    try {
      NotificationTrigger trigger = objectMapper.readValue(payload, NotificationTrigger.class);
      triggerService.process(trigger);
      acknowledgment.acknowledge();
    } catch (NotificationException | RuntimeException exception) {
      // Do not acknowledge. Kafka will redeliver after a transient database failure.
      LOGGER.log(Level.WARNING, "NotificationTrigger was not processed", exception);
    } catch (Exception exception) {
      LOGGER.log(Level.WARNING, "NotificationTrigger payload could not be read", exception);
    }
  }
}
