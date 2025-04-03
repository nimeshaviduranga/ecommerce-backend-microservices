package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.OrderDTO;
import com.ecommerce.paymentservice.dto.PaymentResponseDTO;

public interface OrderServiceClient {

    /**
     * Gets an order by its ID
     *
     * @param orderId The order ID
     * @return The order details
     */
    OrderDTO getOrderById(String orderId);

    /**
     * Updates an order's payment status
     *
     * @param orderId The order ID
     * @param paymentResponse The payment response containing status information
     * @return True if the update was successful
     */
    boolean updateOrderPaymentStatus(String orderId, PaymentResponseDTO paymentResponse);
}