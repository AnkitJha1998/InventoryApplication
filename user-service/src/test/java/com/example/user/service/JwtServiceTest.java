package com.example.user.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
  @Test
  void tokenContainsOnlyRequiredClaimsAndBearerExpiry() throws Exception {
    var service = new JwtService(new ObjectMapper(), "test-secret", 3600);
    UUID id = UUID.randomUUID();
    String token = service.issue(id, "STORE_MANAGER");
    var payload =
        new ObjectMapper()
            .readTree(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1])));
    assertEquals(4, payload.size());
    assertEquals(id.toString(), payload.get("userId").asText());
    assertEquals("STORE_MANAGER", payload.get("role").asText());
    assertTrue(payload.has("iat"));
    assertTrue(payload.has("exp"));
    assertEquals(3600, payload.get("exp").asLong() - payload.get("iat").asLong());
  }
}
