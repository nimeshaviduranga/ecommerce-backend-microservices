package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.PushNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PushNotificationRepository extends JpaRepository<PushNotification, Long> {

    Page<PushNotification> findByUserId(Long userId, Pageable pageable);

    Page<PushNotification> findByDeviceToken(String deviceToken, Pageable pageable);

    Page<PushNotification> findByUserIdAndSent(Long userId, boolean sent, Pageable pageable);
}