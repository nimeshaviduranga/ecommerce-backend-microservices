package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderDTO createOrder(Long userId, OrderCreateRequest orderRequest);

    OrderDTO getOrderById(Long id);

    OrderDTO getOrderByOrderNumber(String orderNumber);

    Page<OrderDTO> getUserOrders(Long userId, Pageable pageable);

    Page<OrderDTO> getUserOrdersByStatus(Long userId, OrderStatus status, Pageable pageable);

    Page<OrderDTO> getAllOrdersByStatus(OrderStatus status, Pageable pageable);

    OrderDTO updateOrderStatus(Long id, OrderStatusUpdateRequest statusUpdateRequest);

    OrderDTO processPayment(Long id, OrderPaymentRequest paymentRequest);

    OrderDTO cancelOrder(Long id, String reason);
}
