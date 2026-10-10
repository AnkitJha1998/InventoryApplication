package com.example.inventory.inventory_creation_bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.inventory.api.CreateRequest;
import com.example.inventory.api.CreateResponse;
import com.example.inventory.api.ReductionResponse;
import com.example.inventory.consumer.InventoryUpdateConsumer;
import com.example.inventory.outbox.OutboxWorker;
import com.example.inventory.persistence.entity.Inventory;
import com.example.inventory.persistence.entity.InventoryDocument;
import com.example.inventory.persistence.entity.OutboxEvent;
import com.example.inventory.persistence.entity.OutboxStatus;
import com.example.inventory.persistence.repository.InventoryDocumentWriter;
import com.example.inventory.persistence.repository.InventoryOperationRepository;
import com.example.inventory.persistence.repository.InventoryRepository;
import com.example.inventory.persistence.repository.OutboxEventRepository;
import com.example.inventory.service.BusinessException;
import com.example.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Instant;
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
    private InventoryOperationRepository operationRepository;

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
    private ReductionResponse reductionResponse;
    private String reductionError;
    private org.springframework.kafka.support.Acknowledgment acknowledgment;
    private org.springframework.kafka.support.Acknowledgment versioningAcknowledgment;

    @Before
    public void resetScenario() {
        outboxRepository.deleteAll();
        operationRepository.deleteAll();
        inventoryRepository.deleteAll();
        reset(kafkaTemplate, documentWriter);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        creationResponse = null;
        reductionResponse = null;
        reductionError = null;
        attemptedProductName = null;
        creationFailed = false;
        publishedPayload = null;
        publishedTopic = null;
        acknowledgment = org.mockito.Mockito.mock(
                org.springframework.kafka.support.Acknowledgment.class);
        versioningAcknowledgment = org.mockito.Mockito.mock(
                org.springframework.kafka.support.Acknowledgment.class);
    }

    @When("an InventoryUpdate event for {string} with version {long} and quantity {int} is consumed")
    public void consumeVersionedEvent(String productId, long version, int quantity) throws Exception {
        String event = objectMapper.writeValueAsString(new com.example.inventory.consumer.InventoryUpdate(
                UUID.randomUUID(), productId, "Versioned Product", "ELECTRONICS", 0,
                quantity, version, "InventoryUpdate", java.time.Instant.now()));
        try {
            consumer.consume(event, versioningAcknowledgment);
        } catch (RuntimeException ignored) {
            // The following assertion verifies acknowledgment state for failure scenarios.
        }
    }

    @Given("the CAS writer is unavailable")
    public void casWriterUnavailable() {
        doThrow(new IllegalStateException("temporarily unavailable"))
                .when(documentWriter).applyIfNewer(any(InventoryDocument.class));
    }

    @Then("the CAS writer should receive product {string} with version {long} and quantity {int}")
    public void casWriterReceivesVersionedProduct(String productId, long version, int quantity) {
        var captor = ArgumentCaptor.forClass(InventoryDocument.class);
        verify(documentWriter).applyIfNewer(captor.capture());
        assertEquals(productId, captor.getValue().getProductId());
        assertEquals(version, captor.getValue().getVersion());
        assertEquals(quantity, captor.getValue().getAvailableQuantity());
    }

    @Then("the InventoryUpdate event should be acknowledged")
    public void versionedEventShouldBeAcknowledged() {
        verify(versioningAcknowledgment).acknowledge();
    }

    @Then("the InventoryUpdate event should not be acknowledged")
    public void versionedEventShouldNotBeAcknowledged() {
        verify(versioningAcknowledgment, never()).acknowledge();
    }

    @Given("inventory product {string} exists with quantity {int}")
    public void inventoryProductExists(String productId, int quantity) {
        inventoryRepository.save(Inventory.builder()
                .productId(productId)
                .productName(productId)
                .productType("ELECTRONICS")
                .availableCount(quantity)
                .version(0)
                .updatedAt(Instant.now())
                .build());
    }

    @When("order {string} reduces product {string} by {int}")
    public void reduceOrder(String orderId, String productId, int quantity) {
        try {
            reductionResponse = inventoryService.reduce(
                    new com.example.inventory.api.ReductionRequest(orderId, productId, quantity));
        } catch (BusinessException exception) {
            reductionError = exception.getMessage();
        }
    }

    @When("order {string} reduces product {string} by {int} again")
    public void reduceOrderAgain(String orderId, String productId, int quantity) {
        reduceOrder(orderId, productId, quantity);
    }

    @Then("the reduction current quantity should be {int}")
    public void reductionCurrentQuantity(int quantity) {
        assertEquals(quantity, reductionResponse.currentCount());
    }

    @Then("one inventory operation should exist for order {string}")
    public void oneOperationShouldExist(String orderId) {
        assertEquals(1, operationRepository.findAll().stream()
                .filter(operation -> operation.getOrderId().equals(orderId)).count());
    }

    @Then("one InventoryUpdate Outbox event should exist for product {string}")
    public void outboxEventShouldExist(String productId) {
        assertEquals(1, outboxRepository.findAll().stream()
                .filter(event -> event.getProductId().equals(productId))
                .count());
    }

    @Then("the second reduction should be marked already processed")
    public void secondReductionAlreadyProcessed() {
        assertTrue(reductionResponse.alreadyProcessed());
    }

    @Then("the product quantity should remain {int}")
    public void productQuantityShouldRemain(int quantity) {
        String productId = reductionResponse == null
                ? (reductionError != null && reductionError.equals("insufficient stock")
                    ? "limited-product" : "idempotent-product")
                : reductionResponse.productId();
        Inventory inventory = inventoryRepository.findById(productId).orElseThrow();
        assertEquals(quantity, inventory.getAvailableCount());
    }

    @Then("the reduction should fail with an idempotency conflict")
    public void reductionIdempotencyConflict() {
        assertTrue(reductionError.contains("idempotency conflict"));
    }

    @Then("the reduction should fail with insufficient stock")
    public void reductionInsufficientStock() {
        assertEquals("insufficient stock", reductionError);
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
