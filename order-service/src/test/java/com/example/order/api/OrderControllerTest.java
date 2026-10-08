package com.example.order.api;

import com.example.order.service.OrderService;
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

@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired
    MockMvc mvc;
    @MockBean
    OrderService service;

    @Test
    void delegatesValidOrder() throws Exception {
        when(service.place(any())).thenReturn(new OrderResponse("o", "p", 1, 4, 3, false));
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"o\",\"productId\":\"p\",\"quantity\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void validatesQuantity() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"o\",\"productId\":\"p\",\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }
}
