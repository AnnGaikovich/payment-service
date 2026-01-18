package org.example.paymentservice.exception;

public class PaymentAlreadyExistsException extends BusinessException {

    public PaymentAlreadyExistsException(Long orderId) {
        super("Payment already exists for order: " + orderId);
    }

    public PaymentAlreadyExistsException(String message) {
        super(message);
    }
}