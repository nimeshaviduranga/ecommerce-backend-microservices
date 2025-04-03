package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.EmailNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {

    Page<EmailNotification> findByUserId(Long userId, Pageable pageable);

    Page<EmailNotification> findByRecipientEmail(String recipientEmail, Pageable pageable);

    Page<EmailNotification> findByUserIdAndSent(Long userId, boolean sent, Pageable pageable);

    List<EmailNotification> findBySentAndHasAttachments(boolean sent, boolean hasAttachments);
}