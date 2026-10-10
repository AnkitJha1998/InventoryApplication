package com.example.inventory.inventory_creation_bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.inventory.api.CreateRequest;
import com.example.inventory.api.CreateResponse;
import com.example.inventory.consumer.InventoryUpdateConsumer;
import com.example.inventory.outbox.OutboxWorker;
import com.example.inventory.persistence.entity.Inventory;
import com.example.inventory.persistence.entity.OutboxEvent;
import com.example.inventory.persistence.entity.OutboxStatus;
import com.example.inventory.persistence.repository.InventoryDocumentWriter;
import com.example.inventory.persistence.repository.InventoryRepository;
import com.example.inventory.persistence.repository.OutboxEventRepository;
import com.example.inventory.service.BusinessException;
import com.example.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

public class InventoryCreationPersistenceSteps {
    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private OutboxWorker outboxWorker;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private InventoryDocumentWriter documentWriter;

    @Autowired
    private InventoryUpdateConsumer consumer;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateResponse creationResponse;
    private String attemptedProductName;
    private boolean creationFailed;
    private String publishedPayload;
    private String publishedTopic;
    private org.springframework.kafka.support.Acknowledgment acknowledgment;

    @Before
    public void resetScenario() {
        outboxRepository.deleteAll();
        inventoryRepository.deleteAll();
        reset(kafkaTemplate, documentWriter);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        creationResponse = null;
        attemptedProductName = null;
        creationFailed = false;
        publishedPayload = null;
        publishedTopic = null;
        acknowledgment = org.mockito.Mockito.mock(
                org.springframework.kafka.support.Acknowledgment.class);
    }

    @When("the service creates product {string} of type {string} with quantity {int}")
    public void createProduct(String productName, String productType, int quantity)
            throws Exception {
        attemptedProductName = productName;
        creationResponse = inventoryService.create(new CreateRequest(productName, productType, quantity));
    }

    @When("the service attempts to create product {string} of type {string} with quantity {int}")
    public void attemptCreateProduct(String productName, String productType, int quantity) {
        attemptedProductName = productName;
        try {
            inventoryService.create(new CreateRequest(productName, productType, quantity));
        } catch (BusinessException exception) {
            creationFailed = true;
        }
    }

    @Then("the created inventory row should exist")
    public void createdInventoryRowShouldExist() {
        assertNotNull(creationResponse);
        assertTrue(inventoryRepository.findById(creationResponse.productId()).isPresent());
    }

    @Then("the stored quantity should be {int}")
    public void storedQuantityShouldBe(int quantity) {
        Inventory inventory = inventoryRepository.findById(creationResponse.productId()).orElseThrow();
        assertEquals(quantity, inventory.getAvailableCount());
    }

    @Then("the stored version should be {int}")
    public void storedVersionShouldBe(long version) {
        Inventory inventory = inventoryRepository.findById(creationResponse.productId()).orElseThrow();
        assertEquals(version, inventory.getVersion());
    }

    @Then("exactly one InventoryUpdate Outbox event should exist for the product")
    public void oneInventoryUpdateEventShouldExist() {
        var events = outboxRepository.findAll().stream()
                .filter(event -> event.getProductId().equals(creationResponse.productId()))
                .toList();
        assertEquals(1, events.size());
        assertEquals("InventoryUpdate", events.getFirst().getTopic());
    }

    @Then("the Outbox event should be pending with previous count {int} and current count {int}")
    public void outboxEventShouldBePending(int previous, int current) {
        OutboxEvent event = outboxRepository.findAll().stream()
                .filter(candidate -> candidate.getProductId().equals(creationResponse.productId()))
                .findFirst()
                .orElseThrow();
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(previous, event.getPreviousCount());
        assertEquals(current, event.getCurrentCount());
        assertEquals(1, event.getVersion());
        assertEquals(0, event.getRetryCount());
    }

    @Then("the creation should fail")
    public void creationShouldFail() {
        assertTrue(creationFailed);
    }

    @Then("no inventory row should exist for the attempted product")
    public void noInventoryRowShouldExist() {
        assertTrue(inventoryRepository.findAll().stream()
                .noneMatch(inventory -> inventory.getProductName().equals(attemptedProductName)));
    }

    @Then("no Outbox event should exist for the attempted product")
    public void noOutboxEventShouldExist() {
        assertTrue(outboxRepository.findAll().stream()
                .noneMatch(event -> event.getProductName().equals(attemptedProductName)));
    }

    @When("the Outbox Worker publishes pending events")
    public void publishPendingEvents() {
        outboxWorker.publishOnce();
        var topicCaptor = ArgumentCaptor.forClass(String.class);
        var payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), anyString(), payloadCaptor.capture());
        publishedTopic = topicCaptor.getValue();
        publishedPayload = payloadCaptor.getValue();
    }

    @Then("the published event should use the InventoryUpdate topic")
    public void publishedEventUsesInventoryUpdateTopic() {
        assertEquals("InventoryUpdate", publishedTopic);
    }

    @Then("the published event should contain the created product id, version 1, previous count 0, and current count 5")
    public void publishedEventContainsInitialState() throws Exception {
        JsonNode event = objectMapper.readTree(publishedPayload);
        assertEquals(creationResponse.productId(), event.get("productId").asText());
        assertEquals(1, event.get("version").asInt());
        assertEquals(0, event.get("previousCount").asInt());
        assertEquals(5, event.get("currentCount").asInt());
    }

    @When("the InventoryUpdate consumer processes the published event")
    public void processPublishedEvent() throws Exception {
        consumer.consume(publishedPayload, acknowledgment);
    }

    @Then("the Couchbase writer should receive the created product with version 1 and quantity 5")
    public void writerReceivesCreatedProduct() {
        var captor = ArgumentCaptor.forClass(com.example.inventory.persistence.entity.InventoryDocument.class);
        verify(documentWriter).applyIfNewer(captor.capture());
        var document = captor.getValue();
        assertEquals(creationResponse.productId(), document.getProductId());
        assertEquals(1, document.getVersion());
        assertEquals(5, document.getAvailableQuantity());
    }

    @Then("the Kafka event should be acknowledged")
    public void kafkaEventShouldBeAcknowledged() {
        verify(acknowledgment).acknowledge();
    }
}
