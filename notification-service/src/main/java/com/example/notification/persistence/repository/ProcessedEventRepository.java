package com.example.notification.persistence.repository;

import com.example.notification.persistence.entity.ProcessedEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {}
