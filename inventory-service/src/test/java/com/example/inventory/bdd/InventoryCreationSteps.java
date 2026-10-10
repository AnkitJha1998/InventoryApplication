package com.example.inventory.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.inventory.api.CreateRequest;
import com.example.inventory.api.CreateResponse;
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

    private MvcResult latestResponse;
    private String authorizationHeader;

    @Before
    public void resetScenario() throws Exception {
        reset(inventoryService);
        when(inventoryService.create(any(CreateRequest.class)))
                .thenReturn(new CreateResponse(
                        UUID.randomUUID().toString(), "Demo Laptop", "ELECTRONICS", 5, 1));
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
