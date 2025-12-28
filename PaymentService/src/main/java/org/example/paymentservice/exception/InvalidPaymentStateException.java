package org.example.paymentservice.exception;

import org.example.paymentservice.enums.PaymentStatus;

import java.util.Arrays;

public class InvalidPaymentStateException extends ValidationException {

    public InvalidPaymentStateException(PaymentStatus status) {
        super("Invalid payment status: '" + status +
                "'. Allowed statuses: " + Arrays.toString(PaymentStatus.values()));
    }

    public InvalidPaymentStateException(String message) {
        super(message);
    }

    public InvalidPaymentStateException(String currentStatus, String operation) {
        super("Cannot " + operation + " for payment in status: " + currentStatus);
    }
}