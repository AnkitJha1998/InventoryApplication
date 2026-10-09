package com.example.notification.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.notification.service.DownstreamQueryException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionHandlerTest {
  @Test
  void downstreamQueryFailureReturnsStructuredInternalServerError() {
    var response =
        new ApiExceptionHandler()
            .downstream(new DownstreamQueryException("inventory query failed", null));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals("inventory query failed", response.getBody().get("message"));
  }
}
