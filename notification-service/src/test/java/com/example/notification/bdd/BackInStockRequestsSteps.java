package com.example.notification.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.notification.api.BackInStockRequestBody;
import com.example.notification.persistence.entity.BackInStockRequest;
import com.example.notification.persistence.entity.BackInStockStatus;
import com.example.notification.service.NotificationException;
import com.example.notification.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public class BackInStockRequestsSteps {
    private static final String JWT_SECRET = "test-signing-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationService notificationService;

    private MvcResult latestResponse;
    private String authorizationHeader;

    @Before
    public void resetScenario() throws Exception {
        reset(notificationService);
        when(notificationService.requestBackInStock(
                any(UUID.class), any(BackInStockRequestBody.class), anyString()))
                .thenReturn(BackInStockRequest.builder()
                        .requestId(UUID.randomUUID())
                        .customerId(UUID.randomUUID())
                        .productId("product-1")
                        .status(BackInStockStatus.ACTIVE)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build());
        latestResponse = null;
        authorizationHeader = null;
    }

    @Given("a back-in-stock CUSTOMER JWT")
    public void customerJwt() {
        authorizationHeader = "Bearer " + createToken("CUSTOMER");
    }

    @Given("a back-in-stock STORE_MANAGER JWT")
    public void managerJwt() {
        authorizationHeader = "Bearer " + createToken("STORE_MANAGER");
    }

    @Given("an invalid back-in-stock JWT")
    public void invalidJwt() {
        authorizationHeader = "Bearer invalid-token";
    }

    @Given("an existing active request for product {string}")
    public void existingActiveRequest(String productId) throws Exception {
        when(notificationService.requestBackInStock(
                any(UUID.class), any(BackInStockRequestBody.class), anyString()))
                .thenThrow(new NotificationException("an active request already exists"));
    }

    @Given("the product is already in stock")
    public void productAlreadyInStock() throws Exception {
        when(notificationService.requestBackInStock(
                any(UUID.class), any(BackInStockRequestBody.class), anyString()))
                .thenThrow(new NotificationException("product is already in stock"));
    }

    @When("the customer requests back-in-stock for product {string}")
    public void requestBackInStock(String productId) throws Exception {
        var request = post("/notifications/back-in-stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BackInStockRequestBody(productId)));
        if (authorizationHeader != null) {
            request.header("Authorization", authorizationHeader);
        }
        latestResponse = mockMvc.perform(request).andReturn();
    }

    @Then("the back-in-stock response status should be {int}")
    public void responseStatus(int status) {
        assertEquals(status, latestResponse.getResponse().getStatus());
    }

    @Then("the response should contain product {string} with status {string}")
    public void responseContains(String productId, String status) throws Exception {
        JsonNode body = objectMapper.readTree(latestResponse.getResponse().getContentAsString());
        assertEquals(productId, body.get("productId").asText());
        assertEquals(status, body.get("status").asText());
    }

    @Then("the Inventory Service should receive the same Authorization header")
    public void authorizationForwarded() throws Exception {
        verify(notificationService).requestBackInStock(
                any(UUID.class), any(BackInStockRequestBody.class), eq(authorizationHeader));
    }

    private String createToken(String role) {
        try {
            long issuedAt = Instant.now().getEpochSecond();
            var header = new LinkedHashMap<String, Object>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            var claims = new LinkedHashMap<String, Object>();
            claims.put("userId", UUID.randomUUID().toString());
            claims.put("role", role);
            claims.put("iat", issuedAt);
            claims.put("exp", issuedAt + 3600);
            String content = encode(objectMapper.writeValueAsBytes(header)) + "."
                    + encode(objectMapper.writeValueAsBytes(claims));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return content + "." + encode(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create test JWT", exception);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
