package com.example.order.bdd;

import com.example.order.security.JwtService;
import com.example.order.security.SecurityConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.example.order.service.OrderService;
import static org.mockito.Mockito.mock;

import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@WebMvcTest
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtService.class, CucumberOrderTestConfiguration.class})
@ActiveProfiles("test")
public class CucumberSpringConfiguration {}

@TestConfiguration
class CucumberOrderTestConfiguration {
    @Bean
    OrderService orderService() {
        return mock(OrderService.class);
    }
}
