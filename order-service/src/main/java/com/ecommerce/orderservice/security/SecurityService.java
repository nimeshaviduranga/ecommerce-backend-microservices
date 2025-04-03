package com.ecommerce.cartservice.security;

import com.ecommerce.orderservice.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for security-related operations and checks
 */
@Service
@Slf4j
public class SecurityService {

    /**
     * Checks if the currently authenticated user has the specified user ID
     *
     * @param userId The user ID to check against
     * @return true if the current user has this ID, false otherwise
     */
    public boolean isCurrentUser(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            log.debug("Checking if current user ID {} matches requested ID {}", principal.getId(), userId);
            return principal.getId().equals(userId);
        }

        log.debug("No authenticated user found when checking for user ID: {}", userId);
        return false;
    }
}