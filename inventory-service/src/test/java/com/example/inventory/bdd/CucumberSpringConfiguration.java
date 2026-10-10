package com.example.inventory.bdd;

import com.example.inventory.security.JwtService;
import com.example.inventory.security.SecurityConfig;
import com.example.inventory.service.InventoryService;
import io.cucumber.spring.CucumberContextConfiguration;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@WebMvcTest
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtService.class, CucumberInventoryTestConfiguration.class})
@ActiveProfiles("test")
public class CucumberSpringConfiguration {}

@TestConfiguration
class CucumberInventoryTestConfiguration {
    @Bean
    InventoryService inventoryService() {
        return Mockito.mock(InventoryService.class);
    }
}
