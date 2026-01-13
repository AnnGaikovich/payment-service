package org.example.paymentservice.exception;

public class PaymentNotFoundException extends BusinessException {

    public PaymentNotFoundException(Long paymentId) {
        super("Payment not found with id: " + paymentId);
    }

    public PaymentNotFoundException(String message) {
        super(message);
    }
}