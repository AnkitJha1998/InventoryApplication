package com.example.order.service;

import com.example.order.api.OrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderServiceTest {
  @Test void delegatesReductionToInventoryEndpoint() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo("http://inventory.test/inventory/reduce"))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andRespond(withSuccess("{\"orderId\":\"o\",\"productId\":\"p\",\"quantity\":2,\"previousCount\":4,\"currentCount\":2,\"alreadyProcessed\":false}",
            org.springframework.http.MediaType.APPLICATION_JSON));

    var service = new OrderServiceImpl(builder, "http://inventory.test");
    var result = service.place(new OrderRequest("o", "p", 2));
    assertEquals(2, result.currentCount());
    server.verify();
  }
}
