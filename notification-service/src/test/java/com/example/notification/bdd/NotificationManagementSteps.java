package com.example.notification.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import com.example.notification.api.MarkNotificationsReadBody;
import com.example.notification.persistence.entity.Notification;
import com.example.notification.persistence.entity.NotificationStatus;
import com.example.notification.service.NotificationException;
import com.example.notification.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public class NotificationManagementSteps {
    private static final String JWT_SECRET = "test-signing-secret";
    private static final UUID NOTIFICATION_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationService notificationService;

    private MvcResult latestResponse;
    private String authorizationHeader;

    @Given("a notification CUSTOMER JWT")
    public void customerJwt() {
        authorizationHeader = "Bearer " + createToken("CUSTOMER");
    }

    @Given("a notification STORE_MANAGER JWT")
    public void managerJwt() {
        authorizationHeader = "Bearer " + createToken("STORE_MANAGER");
    }

    @When("the customer retrieves the unread notification count")
    public void unreadCount() throws Exception {
        when(notificationService.unreadCount(any(UUID.class))).thenReturn(2L);
        latestResponse = mockMvc.perform(get("/notifications/count")
                        .header("Authorization", authorizationHeader))
                .andReturn();
    }

    @When("the customer retrieves notifications page {int} with size {int}")
    public void notificationList(int page, int size) throws Exception {
        if (size == 20) {
            Notification notification = Notification.builder()
                    .notificationId(NOTIFICATION_ID)
                    .customerId(UUID.randomUUID())
                    .productId("product-1")
                    .requestId(UUID.randomUUID())
                    .eventId(UUID.randomUUID())
                    .message("Demo Laptop is now available")
                    .status(NotificationStatus.CREATED)
                    .createdAt(Instant.now())
                    .build();
            when(notificationService.findNotifications(any(UUID.class), any(Integer.class), any(Integer.class)))
                    .thenReturn(new PageImpl<>(List.of(notification)));
        } else {
            when(notificationService.findNotifications(any(UUID.class), any(Integer.class), any(Integer.class)))
                    .thenThrow(new NotificationException("size must be between 1 and 100"));
        }
        latestResponse = mockMvc.perform(get("/notifications")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .header("Authorization", authorizationHeader))
                .andReturn();
    }

    @When("the customer marks notification {string} as read")
    public void markAsRead(String notificationId) throws Exception {
        when(notificationService.markAsRead(any(UUID.class), any(List.class))).thenReturn(1);
        latestResponse = mockMvc.perform(patch("/notifications/read")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MarkNotificationsReadBody(List.of(UUID.fromString(notificationId))))))
                .andReturn();
    }

    @When("the customer submits an empty mark-read request")
    public void emptyMarkAsRead() throws Exception {
        latestResponse = mockMvc.perform(patch("/notifications/read")
                        .header("Authorization", authorizationHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificationIds\":[]}"))
                .andReturn();
    }

    @Then("the notification count response status should be {int}")
    public void countStatus(int status) {
        assertEquals(status, latestResponse.getResponse().getStatus());
    }

    @Then("the unread count should be {int}")
    public void unreadCountShouldBe(long count) throws Exception {
        assertEquals(count, responseBody().get("unreadCount").asLong());
    }

    @Then("the notification list response status should be {int}")
    public void listStatus(int status) {
        assertEquals(status, latestResponse.getResponse().getStatus());
    }

    @Then("the notification list should contain {int} notification")
    public void listContains(int count) throws Exception {
        assertEquals(count, responseBody().get("content").size());
    }

    @Then("the mark-read response status should be {int}")
    public void markReadStatus(int status) {
        assertEquals(status, latestResponse.getResponse().getStatus());
    }

    @Then("the updated notification count should be {int}")
    public void updatedCount(int count) throws Exception {
        assertEquals(count, responseBody().get("updatedCount").asInt());
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
