package com.example.inventory.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.inventory.persistence.OutboxEvent;
import com.example.inventory.persistence.OutboxEventRepository;
import com.example.inventory.persistence.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class OutboxWorkerTest {
  @Test
  void claimUsesLeasedProcessingStatusAndSkipLocked() {
    OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    UUID id = UUID.randomUUID();
    OutboxEvent event =
        OutboxEvent.builder()
            .eventId(id)
            .productId("p")
            .previousCount(5)
            .currentCount(4)
            .topic("InventoryUpdate")
            .status(OutboxStatus.PENDING)
            .retryCount(0)
            .createdAt(Instant.now())
            .build();
    when(outbox.claimCandidates(5, 20)).thenReturn(List.of(event));
    TransactionTemplate transactions = mock(TransactionTemplate.class);
    when(transactions.execute(any()))
        .thenAnswer(
            invocation ->
                invocation
                    .<org.springframework.transaction.support.TransactionCallback<
                            List<OutboxWorker.Event>>>
                        getArgument(0)
                    .doInTransaction(null));

    var worker =
        new OutboxWorker(
            outbox, mock(KafkaTemplate.class), new ObjectMapper(), transactions, 20, 60);
    worker.claim();

    verify(outbox).claimCandidates(5, 20);
    verify(outbox).save(event);
  }
}
