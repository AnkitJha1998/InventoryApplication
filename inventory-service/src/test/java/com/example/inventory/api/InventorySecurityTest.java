package com.example.inventory.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.security.JwtService;
import com.example.inventory.security.SecurityConfig;
import com.example.inventory.service.InventoryService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
@Import(SecurityConfig.class)
class InventorySecurityTest {
  @Autowired MockMvc mvc;
  @MockBean InventoryService service;
  @MockBean JwtService jwtService;

  @Test
  void authenticatedStoreManagerCanReduceInventory() throws Exception {
    when(jwtService.validate("token"))
        .thenReturn(new JwtService.Claims(UUID.randomUUID(), "STORE_MANAGER", 1, Long.MAX_VALUE));
    when(service.reduce(any())).thenReturn(new ReductionResponse("o", "p", 1, 5, 4, false));
    mvc.perform(
            post("/inventory/reduce")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"o\",\"productId\":\"p\",\"quantity\":1}"))
        .andExpect(status().isOk());
  }
}
