package com.example.notification.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private static final String ALGORITHM = "HmacSHA256";
  private final ObjectMapper objectMapper;
  private final byte[] secret;

  public JwtService(ObjectMapper objectMapper, @Value("${jwt.secret}") String secret) {
    this.objectMapper = objectMapper;
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
  }

  public Claims validate(String token) throws JwtValidationException {
    try {
      String[] parts = token.split("\\.", -1);
      if (parts.length != 3
          || !MessageDigest.isEqual(
              sign(parts[0] + "." + parts[1]), parts[2].getBytes(StandardCharsets.UTF_8))) {
        throw new IllegalArgumentException("invalid token");
      }
      Map<String, Object> payload =
          objectMapper.readValue(
              new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8),
              new TypeReference<>() {});
      if (payload.size() != 4
          || !payload.containsKey("userId")
          || !payload.containsKey("role")
          || !payload.containsKey("iat")
          || !payload.containsKey("exp")) {
        throw new IllegalArgumentException("invalid claims");
      }
      UUID userId = UUID.fromString(String.valueOf(payload.get("userId")));
      String role = String.valueOf(payload.get("role"));
      long issuedAt = ((Number) payload.get("iat")).longValue();
      long expiresAt = ((Number) payload.get("exp")).longValue();
      long now = Instant.now().getEpochSecond();
      if (role.isBlank() || issuedAt > now + 60 || expiresAt <= now || expiresAt <= issuedAt) {
        throw new IllegalArgumentException("expired token");
      }
      return new Claims(userId, role, issuedAt, expiresAt);
    } catch (Exception exception) {
      throw new JwtValidationException("invalid token", exception);
    }
  }

  private byte[] sign(String value) throws Exception {
    Mac mac = Mac.getInstance(ALGORITHM);
    mac.init(new SecretKeySpec(secret, ALGORITHM));
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
  }

  public record Claims(UUID userId, String role, long issuedAt, long expiresAt) {}
}
