package com.example.inventory.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.security.JwtService;
import com.example.inventory.security.JwtValidationException;
import com.example.inventory.security.SecurityConfig;
import com.example.inventory.service.InventoryQueryService;
import com.example.inventory.service.InventoryService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
@Import(SecurityConfig.class)
class InventorySecurityTest {
  @Autowired MockMvc mvc;
  @MockitoBean InventoryService service;
  @MockitoBean InventoryQueryService queryService;
  @MockitoBean JwtService jwtService;

  @Test
  void authenticatedCustomerCanQueryInventory() throws Exception {
    when(jwtService.validate("token"))
        .thenReturn(new JwtService.Claims(UUID.randomUUID(), "CUSTOMER", 1, Long.MAX_VALUE));
    when(queryService.findAll(0, 20))
        .thenReturn(new InventoryPageResponse(java.util.List.of(), 0, 20, 0, 0));
    mvc.perform(get("/inventory").header("Authorization", "Bearer token"))
        .andExpect(status().isOk());
  }

  @Test
  void authenticatedRoleWithoutQueryAccessIsForbidden() throws Exception {
    when(jwtService.validate("token"))
        .thenReturn(new JwtService.Claims(UUID.randomUUID(), "ADMIN", 1, Long.MAX_VALUE));
    mvc.perform(get("/inventory").header("Authorization", "Bearer token"))
        .andExpect(status().isForbidden());
  }

  @Test
  void authenticatedStoreManagerCanCreateInventory() throws Exception {
    when(jwtService.validate("token"))
        .thenReturn(new JwtService.Claims(UUID.randomUUID(), "STORE_MANAGER", 1, Long.MAX_VALUE));
    when(service.create(any())).thenReturn(new CreateResponse("p", "Product", "STANDARD", 1, 1));

    mvc.perform(
            post("/inventory/create")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"productId\":\"p\",\"productName\":\"Product\","
                        + "\"productType\":\"STANDARD\",\"quantity\":1}"))
        .andExpect(status().isOk());
  }

  @Test
  void authenticatedCustomerCannotCreateInventory() throws Exception {
    when(jwtService.validate("token"))
        .thenReturn(new JwtService.Claims(UUID.randomUUID(), "CUSTOMER", 1, Long.MAX_VALUE));

    mvc.perform(
            post("/inventory/create")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"productId\":\"p\",\"productName\":\"Product\","
                        + "\"productType\":\"STANDARD\",\"quantity\":1}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void missingJwtCannotCreateInventory() throws Exception {
    mvc.perform(
            post("/inventory/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"productId\":\"p\",\"productName\":\"Product\","
                        + "\"productType\":\"STANDARD\",\"quantity\":1}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void invalidJwtCannotCreateInventory() throws Exception {
    when(jwtService.validate("invalid")).thenThrow(new JwtValidationException("invalid token"));

    mvc.perform(
            post("/inventory/create")
                .header("Authorization", "Bearer invalid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"productId\":\"p\",\"productName\":\"Product\","
                        + "\"productType\":\"STANDARD\",\"quantity\":1}"))
        .andExpect(status().isUnauthorized());
  }

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
