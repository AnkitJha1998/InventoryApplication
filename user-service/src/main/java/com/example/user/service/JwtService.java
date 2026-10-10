package com.example.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.UUID;
import javax.crypto.Mac;
import lombok.extern.slf4j.Slf4j;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JwtService {
  private final ObjectMapper objectMapper;
  private final byte[] secret;
  private final long expirationSeconds;

  public JwtService(
      ObjectMapper objectMapper,
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration-seconds:3600}") long expirationSeconds) {
    this.objectMapper = objectMapper;
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.expirationSeconds = expirationSeconds;
  }

  public String issue(UUID userId, String role) {
    try {
      long iat = Instant.now().getEpochSecond();
      var header = new LinkedHashMap<String, Object>();
      header.put("alg", "HS256");
      header.put("typ", "JWT");
      var claims = new LinkedHashMap<String, Object>();
      claims.put("userId", userId.toString());
      claims.put("role", role);
      claims.put("iat", iat);
      claims.put("exp", iat + expirationSeconds);
      String encodedHeader = encode(objectMapper.writeValueAsBytes(header));
      String encodedClaims = encode(objectMapper.writeValueAsBytes(claims));
      String content = encodedHeader + "." + encodedClaims;
      return content + "." + new String(sign(content), StandardCharsets.US_ASCII);
    } catch (Exception e) {
      log.error("[issue][{}] access token creation failed userId={}", Instant.now(), userId, e);
      throw new IllegalStateException("could not create access token", e);
    }
  }

  public long expiresIn() {
    return expirationSeconds;
  }

  private String encode(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private byte[] sign(String value) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret, "HmacSHA256"));
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
  }
}
