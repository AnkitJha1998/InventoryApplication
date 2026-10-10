package com.example.inventory.inventory_creation_bdd;

import com.example.inventory.consumer.InventoryUpdateConsumer;
import com.example.inventory.outbox.OutboxWorker;
import com.example.inventory.persistence.repository.InventoryDocumentWriter;
import com.example.inventory.service.InventoryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.CucumberContextConfiguration;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@DataJpaTest
@Import({
    InventoryServiceImpl.class,
    OutboxWorker.class,
    InventoryUpdateConsumer.class,
    CucumberPersistenceTestConfiguration.class
})
@ActiveProfiles("bdd")
public class CucumberSpringConfiguration {}

@TestConfiguration
class CucumberPersistenceTestConfiguration {
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    KafkaTemplate<String, String> kafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }

    @Bean
    InventoryDocumentWriter inventoryDocumentWriter() {
        return Mockito.mock(InventoryDocumentWriter.class);
    }
}
