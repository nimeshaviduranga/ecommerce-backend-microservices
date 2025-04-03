package com.ecommerce.paymentservice.security;

import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.Refund;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    /**
     * Checks if the currently authenticated user can access a payment
     *
     * @param paymentId The payment ID
     * @return true if the user can access the payment
     */
    public boolean canAccessPayment(String paymentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Admins can access all payments
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true;
        }

        // Check if the user is the owner of the payment
        if (authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            Optional<Payment> paymentOpt = paymentRepository.findByPaymentId(paymentId);
            if (paymentOpt.isPresent()) {
                return paymentOpt.get().getUserId().equals(principal.getId());
            }
        }

        return false;
    }

    /**
     * Checks if the currently authenticated user can access a refund
     *
     * @param refundId The refund ID
     * @return true if the user can access the refund
     */
    public boolean canAccessRefund(String refundId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Admins can access all refunds
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true;
        }

        // Check if the user is the owner of the payment associated with the refund
        if (authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            Optional<Refund> refundOpt = refundRepository.findByRefundId(refundId);
            if (refundOpt.isPresent()) {
                return refundOpt.get().getPayment().getUserId().equals(principal.getId());
            }
        }

        return false;
    }

    /**
     * Checks if the currently authenticated user can access an order
     *
     * @param orderId The order ID
     * @return true if the user can access the order
     */
    public boolean canAccessOrder(String orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Admins can access all orders
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true;
        }

        // Check if the user is the owner of the payment associated with the order
        if (authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
            if (paymentOpt.isPresent()) {
                return paymentOpt.get().getUserId().equals(principal.getId());
            }
        }

        return false;
    }

    /**
     * Checks if the currently authenticated user is an admin
     *
     * @return true if the user is an admin
     */
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}