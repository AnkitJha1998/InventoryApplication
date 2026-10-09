package com.example.notification.consumer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.notification.service.NotificationException;
import com.example.notification.service.NotificationTriggerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

class NotificationTriggerConsumerTest {
  @Test
  void databaseFailureDoesNotAcknowledge() throws Exception {
    NotificationTriggerService service = mock(NotificationTriggerService.class);
    ObjectMapper mapper = new ObjectMapper();
    NotificationTriggerConsumer consumer = new NotificationTriggerConsumer(mapper, service);
    Acknowledgment acknowledgment = mock(Acknowledgment.class);
    NotificationTrigger trigger =
        new NotificationTrigger(
            UUID.randomUUID(),
            "P1",
            "Demo",
            "STANDARD",
            0,
            2,
            3,
            Instant.now(),
            "NotificationTrigger");
    doThrow(new NotificationException("database unavailable")).when(service).process(trigger);

    consumer.onMessage(mapper.writeValueAsString(trigger), acknowledgment);

    verify(acknowledgment, never()).acknowledge();
  }
}
