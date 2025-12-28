package org.example.paymentservice.exception;

public class UnauthorizedAccessException extends BusinessException {

    public UnauthorizedAccessException(String operation, Long targetUserId) {
        super("Access denied: Cannot " + operation + " for user " + targetUserId);
    }

    public UnauthorizedAccessException(String operation) {
        super("Access denied: Admin role required to " + operation);
    }
}