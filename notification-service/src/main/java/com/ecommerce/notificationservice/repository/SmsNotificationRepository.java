package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.SmsNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmsNotificationRepository extends JpaRepository<SmsNotification, Long> {

    Page<SmsNotification> findByUserId(Long userId, Pageable pageable);

    Page<SmsNotification> findByPhoneNumber(String phoneNumber, Pageable pageable);

    Page<SmsNotification> findByUserIdAndSent(Long userId, boolean sent, Pageable pageable);
}