package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.Notification;
import com.ecommerce.notificationservice.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndType(Long userId, NotificationType type, Pageable pageable);

    Page<Notification> findByUserIdAndSent(Long userId, boolean sent, Pageable pageable);

    Page<Notification> findByUserIdAndTypeAndSent(Long userId, NotificationType type, boolean sent, Pageable pageable);

    Page<Notification> findBySent(boolean sent, Pageable pageable);

    List<Notification> findBySentAndCreatedAtBefore(boolean sent, LocalDateTime createdAt);

    long countByUserIdAndSent(Long userId, boolean sent);

    long countByUserIdAndTypeAndSent(Long userId, NotificationType type, boolean sent);
}