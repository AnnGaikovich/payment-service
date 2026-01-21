package org.example.paymentservice.exception;

public class InvalidDateRangeException extends ValidationException {

    public InvalidDateRangeException() {
        super("Start date must be before end date");
    }

    public InvalidDateRangeException(String message) {
        super(message);
    }
}