package com.example.inventory.api_bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.inventory.api.CreateRequest;
import com.example.inventory.api.CreateResponse;
import com.example.inventory.api.IncreaseRequest;
import com.example.inventory.api.IncreaseResponse;
import com.example.inventory.api.InventoryResponse;
import com.example.inventory.service.BusinessException;
import com.example.inventory.service.InventoryNotFoundException;
import com.example.inventory.service.InventoryQueryService;
import com.example.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
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

public class InventoryCreationSteps {
    private static final String JWT_SECRET = "test-signing-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryQueryService queryService;

    private MvcResult latestResponse;
    private String authorizationHeader;

    @Before
    public void resetScenario() throws Exception {
        reset(inventoryService, queryService);
        when(inventoryService.create(any(CreateRequest.class)))
                .thenReturn(new CreateResponse(
                        UUID.randomUUID().toString(), "Demo Laptop", "ELECTRONICS", 5, 1));
        when(queryService.findByProductId("product-1"))
                .thenReturn(new InventoryResponse(
                        "product-1", "Demo Laptop", "ELECTRONICS", 5,
                        Instant.parse("2026-01-01T00:00:00Z"), 5, true, 1));
        when(queryService.findByProductId("missing-product"))
                .thenThrow(new InventoryNotFoundException("missing-product"));
        when(queryService.findAll(0, 101))
                .thenThrow(new BusinessException(
                        "page must be non-negative and size must be between 1 and 100"));
        when(inventoryService.increase(any(IncreaseRequest.class)))
                .thenReturn(new IncreaseResponse("product-1", 7, 2));
        latestResponse = null;
        authorizationHeader = null;
    }

    @Given("a valid STORE_MANAGER JWT")
    public void validStoreManagerJwt() {
        authorizationHeader = "Bearer " + createToken("STORE_MANAGER");
    }

    @Given("a valid CUSTOMER JWT")
    public void validCustomerJwt() {
        authorizationHeader = "Bearer " + createToken("CUSTOMER");
    }

    @Given("an invalid JWT")
    public void invalidJwt() {
        authorizationHeader = "Bearer invalid-token";
    }

    @Given("a query CUSTOMER JWT")
    public void queryCustomerJwt() {
        authorizationHeader = "Bearer " + createToken("CUSTOMER");
    }

    @Given("a query STORE_MANAGER JWT")
    public void queryStoreManagerJwt() {
        authorizationHeader = "Bearer " + createToken("STORE_MANAGER");
    }

    @When("the manager creates product {string} of type {string} with quantity {int}")
    public void createProduct(String productName, String productType, int quantity) throws Exception {
        var request = new CreateRequest(productName, productType, quantity);
        var builder = post("/inventory/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
        if (authorizationHeader != null) {
            builder.header("Authorization", authorizationHeader);
        }
        latestResponse = mockMvc.perform(builder).andReturn();
    }

    @When("the customer queries product {string}")
    public void queryProduct(String productId) throws Exception {
        latestResponse = mockMvc.perform(get("/inventory/" + productId)
                        .header("Authorization", authorizationHeader))
                .andReturn();
    }

    @When("the customer browses inventory with page {int} and size {int}")
    public void browseInventory(int page, int size) throws Exception {
        latestResponse = mockMvc.perform(get("/inventory")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .header("Authorization", authorizationHeader))
                .andReturn();
    }

    @When("the manager increases product {string} by {int}")
    public void increaseProduct(String productId, int quantity) throws Exception {
        latestResponse = mockMvc.perform(post("/inventory/increase")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new IncreaseRequest(productId, quantity))))
                .andReturn();
    }

    @Then("the inventory creation response status should be {int}")
    public void responseStatus(int status) {
        assertEquals(status, latestResponse.getResponse().getStatus());
    }

    @Then("the response should contain a generated product id")
    public void responseContainsProductId() throws Exception {
        String productId = responseBody().get("productId").asText();
        assertTrue(productId.matches("[0-9a-fA-F-]{36}"));
    }

    @Then("the response version should be 1")
    public void responseVersionIsOne() throws Exception {
        assertEquals(1, responseBody().get("version").asInt());
    }

    @Then("the inventory query response status should be {int}")
    public void queryResponseStatus(int status) {
        assertEquals(status, latestResponse.getResponse().getStatus());
    }

    @Then("the inventory response should contain product {string} with quantity {int} and version {int}")
    public void inventoryResponseContains(String productId, int quantity, int version)
            throws Exception {
        JsonNode body = responseBody();
        assertEquals(productId, body.get("productId").asText());
        assertEquals(quantity, body.get("availableQuantity").asInt());
        assertEquals(version, body.get("version").asInt());
    }

    @Then("the increase response should contain quantity {int} and version {int}")
    public void increaseResponseContains(int quantity, int version) throws Exception {
        JsonNode body = responseBody();
        assertEquals(quantity, body.get("availableQuantity").asInt());
        assertEquals(version, body.get("version").asInt());
    }

    private JsonNode responseBody() throws Exception {
        return objectMapper.readTree(latestResponse.getResponse().getContentAsString());
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
            String encodedHeader = encode(objectMapper.writeValueAsBytes(header));
            String encodedClaims = encode(objectMapper.writeValueAsBytes(claims));
            String content = encodedHeader + "." + encodedClaims;
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
