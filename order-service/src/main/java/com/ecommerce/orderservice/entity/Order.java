package com.ecommerce.orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Order number visible to customers
    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal shippingCost = BigDecimal.ZERO;

    private String paymentMethod;

    private String paymentStatus;

    private String trackingNumber;

    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "shipping_address_id", referencedColumnName = "id")
    private OrderAddress shippingAddress;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "billing_address_id", referencedColumnName = "id")
    private OrderAddress billingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderPayment> payments = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Helper method to add an item
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        recalculateAmounts();
    }

    // Helper method to remove an item
    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
        recalculateAmounts();
    }

    // Helper method to add a payment
    public void addPayment(OrderPayment payment) {
        payments.add(payment);
        payment.setOrder(this);
    }

    // Calculate totals based on items
    public void recalculateAmounts() {
        this.subtotal = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Simple tax calculation (can be made more sophisticated)
        this.tax = this.subtotal.multiply(new BigDecimal("0.05"));

        // Simple flat shipping cost (can be made more sophisticated)
        this.shippingCost = items.isEmpty() ? BigDecimal.ZERO : new BigDecimal("5.00");

        // Calculate total
        this.totalAmount = this.subtotal.add(this.tax).add(this.shippingCost);
    }
}
