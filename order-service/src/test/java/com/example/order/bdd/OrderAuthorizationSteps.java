package com.example.order.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.order.api.OrderRequest;
import com.example.order.api.OrderResponse;
import com.example.order.service.OrderService;
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

public class OrderAuthorizationSteps {
    private static final String JWT_SECRET = "test-signing-secret";
    private static final String AUTHORIZATION_HEADER = "Bearer customer-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderService orderService;

    private MvcResult latestResponse;
    private String authorizationHeader;

    @Before
    public void resetScenario() throws Exception {
        reset(orderService);
        when(orderService.place(any(OrderRequest.class), any(String.class)))
                .thenReturn(new OrderResponse("order-1", "product-1", 1, 5, 4, false));
        latestResponse = null;
        authorizationHeader = null;
    }

    @Given("a valid CUSTOMER JWT")
    public void validCustomerJwt() {
        authorizationHeader = "Bearer " + createToken("CUSTOMER");
    }

    @Given("a valid STORE_MANAGER JWT")
    public void validStoreManagerJwt() {
        authorizationHeader = "Bearer " + createToken("STORE_MANAGER");
    }

    @Given("an invalid JWT")
    public void invalidJwt() {
        authorizationHeader = AUTHORIZATION_HEADER;
    }

    @When("the customer creates an order")
    public void createOrder() throws Exception {
        var request = new OrderRequest("order-1", "product-1", 1);
        var builder = post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
        if (authorizationHeader != null) {
            builder.header("Authorization", authorizationHeader);
        }
        latestResponse = mockMvc.perform(builder).andReturn();
    }

    @Then("the order response status should be {int}")
    public void responseStatus(int status) {
        assertEquals(status, latestResponse.getResponse().getStatus());
    }

    @Then("the Order Service should receive the same Authorization header")
    public void forwardsAuthorizationHeader() throws Exception {
        verify(orderService).place(any(OrderRequest.class), eq(authorizationHeader));
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
            String signature = encode(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
            return content + "." + signature;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create test JWT", exception);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
