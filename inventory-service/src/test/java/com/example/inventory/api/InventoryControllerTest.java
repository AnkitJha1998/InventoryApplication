package com.example.inventory.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.service.InventoryQueryService;
import com.example.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean InventoryService service;
  @MockitoBean InventoryQueryService queryService;

  @Test
  void createsInventoryWithInitialVersion() throws Exception {
    when(service.create(any())).thenReturn(
        new CreateResponse("p", "Product", "STANDARD", 5, 1));

    mvc.perform(
            post("/inventory/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":\"p\",\"productName\":\"Product\","
                        + "\"productType\":\"STANDARD\",\"quantity\":5}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productId").value("p"))
        .andExpect(jsonPath("$.availableQuantity").value(5))
        .andExpect(jsonPath("$.version").value(1));
  }

  @Test
  void rejectsInvalidCreationRequest() throws Exception {
    mvc.perform(
            post("/inventory/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":\"p\",\"productName\":\"\","
                        + "\"productType\":\"STANDARD\",\"quantity\":0}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reducesWithValidRequest() throws Exception {
    when(service.reduce(any())).thenReturn(new ReductionResponse("o", "p", 2, 5, 3, false));
    mvc.perform(
            post("/inventory/reduce")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"o\",\"productId\":\"p\",\"quantity\":2}"))
        .andExpect(status().isOk());
  }

  @Test
  void queriesPagedCatalogue() throws Exception {
    when(queryService.findAll(0, 20))
        .thenReturn(new InventoryPageResponse(java.util.List.of(), 0, 20, 0, 0));
    mvc.perform(get("/inventory"))
        .andExpect(status().isOk());
  }

  @Test
  void rejectsNonPositiveQuantity() throws Exception {
    mvc.perform(
            post("/inventory/reduce")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"o\",\"productId\":\"p\",\"quantity\":0}"))
        .andExpect(status().isBadRequest());
  }
}
