package com.example.order.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.order.security.JwtService;
import com.example.order.security.SecurityConfig;
import com.example.order.service.OrderService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderSecurityTest {
  @Autowired MockMvc mvc;
  @MockBean OrderService service;
  @MockBean JwtService jwtService;

  @Test
  void missingTokenIsUnauthorized() throws Exception {
    mvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"o\",\"productId\":\"p\",\"quantity\":1}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void storeManagerTokenIsForbidden() throws Exception {
    when(jwtService.validate("token"))
        .thenReturn(new JwtService.Claims(UUID.randomUUID(), "STORE_MANAGER", 1, Long.MAX_VALUE));
    mvc.perform(
            post("/orders")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"o\",\"productId\":\"p\",\"quantity\":1}"))
        .andExpect(status().isForbidden());
  }
}
