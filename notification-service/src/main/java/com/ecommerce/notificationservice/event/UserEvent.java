package com.ecommerce.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {

    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;

    private Long userId;
    private String username;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
}