package com.example.notification.persistence.repository;

import com.example.notification.persistence.entity.Notification;
import com.example.notification.persistence.entity.NotificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  long countByCustomerIdAndStatus(UUID customerId, NotificationStatus status);

  Page<Notification> findByCustomerId(UUID customerId, Pageable pageable);

  @Modifying
  @Query(
      "update Notification n set n.status = :notified, n.readAt = :readAt "
          + "where n.customerId = :customerId and n.notificationId in :notificationIds "
          + "and n.status = :created")
  int markAsRead(
      @Param("customerId") UUID customerId,
      @Param("notificationIds") List<UUID> notificationIds,
      @Param("readAt") Instant readAt,
      @Param("created") NotificationStatus created,
      @Param("notified") NotificationStatus notified);
}
