package com.ecommerce.orderservice.service;


import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.*;
import com.ecommerce.orderservice.exception.BadRequestException;
import com.ecommerce.orderservice.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartServiceClient cartServiceClient;
    private final ProductServiceClient productServiceClient;
    private final PaymentService paymentService;

    @Override
    @Transactional
    public OrderDTO createOrder(Long userId, OrderCreateRequest orderRequest) {
        log.debug("Creating order for user ID: {}", userId);

        // 1. Get the user's cart
        CartResponse cart = cartServiceClient.getCartByUserId(userId);

        // 2. Check if cart is empty
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot create order with an empty cart");
        }

        // 3. Create a new order
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.CREATED);
        order.setNotes(orderRequest.getNotes());

        // 4. Set shipping address
        OrderAddress shippingAddress = mapToOrderAddress(orderRequest.getShippingAddress());
        order.setShippingAddress(shippingAddress);

        // 5. Set billing address (use shipping address if not provided)
        OrderAddress billingAddress = orderRequest.getBillingAddress() != null
                ? mapToOrderAddress(orderRequest.getBillingAddress())
                : mapToOrderAddress(orderRequest.getShippingAddress());
        billingAddress.setAddressType("BILLING");
        order.setBillingAddress(billingAddress);

        // 6. Add items from cart to order
        for (CartItemResponse cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setProductName(cartItem.getProductName());
            orderItem.setProductImage(cartItem.getProductImage());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            orderItem.setOrder(order);
            order.getItems().add(orderItem);
        }

        // 7. Calculate totals
        order.recalculateAmounts();

        // 8. Save the order
        Order savedOrder = orderRepository.save(order);

        // 9. Clear the cart
        cartServiceClient.clearCart(userId);

        return mapToDTO(savedOrder);
    }

    @Override
    public OrderDTO getOrderById(Long id) {
        log.debug("Getting order by ID: {}", id);
        Order order = findOrderById(id);
        return mapToDTO(order);
    }

    @Override
    public OrderDTO getOrderByOrderNumber(String orderNumber) {
        log.debug("Getting order by order number: {}", orderNumber);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return mapToDTO(order);
    }

    @Override
    public Page<OrderDTO> getUserOrders(Long userId, Pageable pageable) {
        log.debug("Getting orders for user ID: {}", userId);
        return orderRepository.findByUserId(userId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public Page<OrderDTO> getUserOrdersByStatus(Long userId, OrderStatus status, Pageable pageable) {
        log.debug("Getting orders for user ID: {} with status: {}", userId, status);
        return orderRepository.findByUserIdAndStatus(userId, status, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public Page<OrderDTO> getAllOrdersByStatus(OrderStatus status, Pageable pageable) {
        log.debug("Getting all orders with status: {}", status);
        return orderRepository.findByStatus(status, pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long id, OrderStatusUpdateRequest statusUpdateRequest) {
        log.debug("Updating status for order ID: {} to {}", id, statusUpdateRequest.getStatus());

        Order order = findOrderById(id);

        // Validate status transition
        validateStatusTransition(order.getStatus(), statusUpdateRequest.getStatus());

        // Update status
        order.setStatus(statusUpdateRequest.getStatus());

        // Update notes if provided
        if (statusUpdateRequest.getNotes() != null && !statusUpdateRequest.getNotes().isEmpty()) {
            order.setNotes(statusUpdateRequest.getNotes());
        }

        // If status is SHIPPED, generate a tracking number if not already present
        if (statusUpdateRequest.getStatus() == OrderStatus.SHIPPED && (order.getTrackingNumber() == null || order.getTrackingNumber().isEmpty())) {
            order.setTrackingNumber(generateTrackingNumber());
        }

        // Save the updated order
        Order updatedOrder = orderRepository.save(order);

        return mapToDTO(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDTO processPayment(Long id, OrderPaymentRequest paymentRequest) {
        log.debug("Processing payment for order ID: {}", id);

        Order order = findOrderById(id);

        // Validate order is in CREATED status
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BadRequestException("Cannot process payment for order with status: " + order.getStatus());
        }

        // Process payment
        String transactionId = paymentService.processPayment(paymentRequest);

        // Create payment record
        OrderPayment payment = new OrderPayment();
        payment.setOrder(order);
        payment.setPaymentMethod(paymentRequest.getPaymentMethod());
        payment.setTransactionId(transactionId);
        payment.setAmount(paymentRequest.getAmount());
        payment.setStatus("SUCCESS");
        payment.setPaymentDate(LocalDateTime.now());

        // Add payment to order
        order.addPayment(payment);

        // Update order status
        order.setStatus(OrderStatus.PAID);
        order.setPaymentMethod(paymentRequest.getPaymentMethod());
        order.setPaymentStatus("PAID");

        // Save the updated order
        Order updatedOrder = orderRepository.save(order);

        // Update product stock (in a real implementation, this would be done via events)
        for (OrderItem item : order.getItems()) {
            productServiceClient.updateProductStock(item.getProductId(), -item.getQuantity());
        }

        return mapToDTO(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDTO cancelOrder(Long id, String reason) {
        log.debug("Cancelling order ID: {} with reason: {}", id, reason);

        Order order = findOrderById(id);

        // Validate order can be cancelled
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot cancel order with status: " + order.getStatus());
        }

        // If order was already paid, create a refund (in a real implementation)
        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.PROCESSING) {
            // Process refund logic would go here

            // Return items to inventory
            for (OrderItem item : order.getItems()) {
                productServiceClient.updateProductStock(item.getProductId(), item.getQuantity());
            }
        }

        // Update order status
        order.setStatus(OrderStatus.CANCELLED);
        order.setNotes(reason);

        // Save the updated order
        Order updatedOrder = orderRepository.save(order);

        return mapToDTO(updatedOrder);
    }

    // Helper methods

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }

    private OrderAddress mapToOrderAddress(OrderAddressDTO addressDTO) {
        OrderAddress address = new OrderAddress();
        address.setFullName(addressDTO.getFullName());
        address.setAddressLine1(addressDTO.getAddressLine1());
        address.setAddressLine2(addressDTO.getAddressLine2());
        address.setCity(addressDTO.getCity());
        address.setState(addressDTO.getState());
        address.setPostalCode(addressDTO.getPostalCode());
        address.setCountry(addressDTO.getCountry());
        address.setPhoneNumber(addressDTO.getPhoneNumber());
        address.setAddressType(addressDTO.getAddressType());
        return address;
    }

    private OrderDTO mapToDTO(Order order) {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(order.getId());
        orderDTO.setOrderNumber(order.getOrderNumber());
        orderDTO.setUserId(order.getUserId());
        orderDTO.setStatus(order.getStatus());
        orderDTO.setTotalAmount(order.getTotalAmount());
        orderDTO.setSubtotal(order.getSubtotal());
        orderDTO.setTax(order.getTax());
        orderDTO.setShippingCost(order.getShippingCost());
        orderDTO.setPaymentMethod(order.getPaymentMethod());
        orderDTO.setPaymentStatus(order.getPaymentStatus());
        orderDTO.setTrackingNumber(order.getTrackingNumber());
        orderDTO.setNotes(order.getNotes());
        orderDTO.setCreatedAt(order.getCreatedAt());
        orderDTO.setUpdatedAt(order.getUpdatedAt());

        // Map items
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            orderDTO.setItems(order.getItems().stream()
                    .map(this::mapItemToDTO)
                    .collect(Collectors.toList()));
        }

        // Map addresses
        if (order.getShippingAddress() != null) {
            orderDTO.setShippingAddress(mapAddressToDTO(order.getShippingAddress()));
        }

        if (order.getBillingAddress() != null) {
            orderDTO.setBillingAddress(mapAddressToDTO(order.getBillingAddress()));
        }

        // Map payments
        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            orderDTO.setPayments(order.getPayments().stream()
                    .map(this::mapPaymentToDTO)
                    .collect(Collectors.toList()));
        }

        return orderDTO;
    }

    private OrderItemDTO mapItemToDTO(OrderItem item) {
        OrderItemDTO itemDTO = new OrderItemDTO();
        itemDTO.setId(item.getId());
        itemDTO.setProductId(item.getProductId());
        itemDTO.setProductName(item.getProductName());
        itemDTO.setProductImage(item.getProductImage());
        itemDTO.setQuantity(item.getQuantity());
        itemDTO.setPrice(item.getPrice());
        itemDTO.setSubtotal(item.getSubtotal());
        return itemDTO;
    }

    private OrderAddressDTO mapAddressToDTO(OrderAddress address) {
        OrderAddressDTO addressDTO = new OrderAddressDTO();
        addressDTO.setId(address.getId());
        addressDTO.setFullName(address.getFullName());
        addressDTO.setAddressLine1(address.getAddressLine1());
        addressDTO.setAddressLine2(address.getAddressLine2());
        addressDTO.setCity(address.getCity());
        addressDTO.setState(address.getState());
        addressDTO.setPostalCode(address.getPostalCode());
        addressDTO.setCountry(address.getCountry());
        addressDTO.setPhoneNumber(address.getPhoneNumber());
        addressDTO.setAddressType(address.getAddressType());
        return addressDTO;
    }

    private OrderPaymentDTO mapPaymentToDTO(OrderPayment payment) {
        OrderPaymentDTO paymentDTO = new OrderPaymentDTO();
        paymentDTO.setId(payment.getId());
        paymentDTO.setPaymentMethod(payment.getPaymentMethod());
        paymentDTO.setTransactionId(payment.getTransactionId());
        paymentDTO.setAmount(payment.getAmount());
        paymentDTO.setStatus(payment.getStatus());
        paymentDTO.setPaymentDate(payment.getPaymentDate());
        paymentDTO.setCreatedAt(payment.getCreatedAt());
        paymentDTO.setUpdatedAt(payment.getUpdatedAt());
        return paymentDTO;
    }

    private String generateOrderNumber() {
        // Format: ORD-YYYYMMDDHHmm-XXXX where XXXX is a random alphanumeric string
        String timestamp = LocalDateTime.now().toString().replaceAll("[^0-9]", "").substring(0, 12);
        String random = RandomStringUtils.randomAlphanumeric(4).toUpperCase();
        return "ORD-" + timestamp + "-" + random;
    }

    private String generateTrackingNumber() {
        // Format: TRK-XXXXXXXXXXXX where X is a random alphanumeric character
        return "TRK-" + RandomStringUtils.randomAlphanumeric(12).toUpperCase();
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // This is a simplified validation - in a real system, you'd have a more sophisticated state machine
        switch (currentStatus) {
            case CREATED:
                if (newStatus != OrderStatus.PAID && newStatus != OrderStatus.CANCELLED) {
                    throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
                }
                break;
            case PAID:
                if (newStatus != OrderStatus.PROCESSING && newStatus != OrderStatus.CANCELLED) {
                    throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
                }
                break;
            case PROCESSING:
                if (newStatus != OrderStatus.SHIPPED && newStatus != OrderStatus.CANCELLED) {
                    throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
                }
                break;
            case SHIPPED:
                if (newStatus != OrderStatus.DELIVERED) {
                    throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
                }
                break;
            case DELIVERED:
                throw new BadRequestException("Cannot change status of a delivered order");
            case CANCELLED:
                throw new BadRequestException("Cannot change status of a cancelled order");
            case REFUNDED:
                throw new BadRequestException("Cannot change status of a refunded order");
            default:
                throw new BadRequestException("Unknown order status: " + currentStatus);
        }
    }
}