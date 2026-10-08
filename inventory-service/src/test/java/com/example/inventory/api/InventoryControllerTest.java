package com.example.inventory.api;

import com.example.inventory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {
  @Autowired MockMvc mvc;
  @MockBean InventoryService service;

  @Test void reducesWithValidRequest() throws Exception {
    when(service.reduce(any())).thenReturn(new ReductionResponse("o", "p", 2, 5, 3, false));
    mvc.perform(post("/inventory/reduce").contentType(MediaType.APPLICATION_JSON)
        .content("{\"orderId\":\"o\",\"productId\":\"p\",\"quantity\":2}"))
        .andExpect(status().isOk());
  }

  @Test void rejectsNonPositiveQuantity() throws Exception {
    mvc.perform(post("/inventory/reduce").contentType(MediaType.APPLICATION_JSON)
        .content("{\"orderId\":\"o\",\"productId\":\"p\",\"quantity\":0}"))
        .andExpect(status().isBadRequest());
  }
}
