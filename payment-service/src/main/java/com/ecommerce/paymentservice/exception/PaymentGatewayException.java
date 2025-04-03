package com.ecommerce.paymentservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class PaymentGatewayException extends RuntimeException {

    private final String errorCode;

    public PaymentGatewayException(String message) {
        super(message);
        this.errorCode = "GATEWAY_ERROR";
    }

    public PaymentGatewayException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GATEWAY_ERROR";
    }

    public PaymentGatewayException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}