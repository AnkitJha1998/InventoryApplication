package com.example.notification.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {
  @Id
  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;
}
