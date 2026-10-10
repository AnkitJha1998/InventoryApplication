package com.example.notification.bdd;

import com.example.notification.security.JwtService;
import com.example.notification.security.SecurityConfig;
import com.example.notification.service.NotificationService;
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
@Import({SecurityConfig.class, JwtService.class, CucumberNotificationTestConfiguration.class})
@ActiveProfiles("test")
public class CucumberSpringConfiguration {}

@TestConfiguration
class CucumberNotificationTestConfiguration {
    @Bean
    NotificationService notificationService() {
        return Mockito.mock(NotificationService.class);
    }
}
