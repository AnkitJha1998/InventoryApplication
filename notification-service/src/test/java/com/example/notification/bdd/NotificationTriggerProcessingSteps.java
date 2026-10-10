package com.example.notification.bdd;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.notification.consumer.NotificationTriggerConsumer;
import com.example.notification.service.NotificationException;
import com.example.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.Acknowledgment;

public class NotificationTriggerProcessingSteps {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationService notificationService;

    private Acknowledgment acknowledgment;
    private String payload;

    @Given("a trigger notification CUSTOMER context")
    public void triggerContext() throws Exception {
        reset(notificationService);
        acknowledgment = org.mockito.Mockito.mock(Acknowledgment.class);
        payload = objectMapper.writeValueAsString(new com.example.notification.consumer.NotificationTrigger(
                UUID.randomUUID(), "product-1", "Demo Laptop", "ELECTRONICS", 0, 5, 2,
                Instant.now(), "NotificationTrigger"));
    }

    @Given("the notification processor succeeds")
    public void processorSucceeds() {
        // The mock has no behavior, so NotificationService.process completes successfully.
    }

    @Given("the notification processor fails")
    public void processorFails() throws Exception {
        doThrow(new NotificationException("temporary database failure"))
                .when(notificationService).process(any());
    }

    @When("the NotificationTrigger event is consumed")
    public void consumeTrigger() {
        new NotificationTriggerConsumer(objectMapper, notificationService)
                .onMessage(payload, acknowledgment);
    }

    @When("a malformed NotificationTrigger event is consumed")
    public void consumeMalformedTrigger() {
        new NotificationTriggerConsumer(objectMapper, notificationService)
                .onMessage("not-json", acknowledgment);
    }

    @Then("the notification trigger should be acknowledged")
    public void triggerAcknowledged() throws Exception {
        verify(acknowledgment).acknowledge();
        verify(notificationService).process(any());
    }

    @Then("the malformed trigger should be acknowledged")
    public void malformedAcknowledged() {
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(notificationService);
    }

    @Then("the failed notification trigger should not be acknowledged")
    public void failedTriggerNotAcknowledged() throws Exception {
        verify(acknowledgment, never()).acknowledge();
        verify(notificationService).process(any());
    }
}
