package org.example.paymentservice.exception;

public class ExternalServiceException extends BusinessException {

    public ExternalServiceException(String serviceName) {
        super("External service unavailable: " + serviceName);
    }

    public ExternalServiceException(String serviceName, Throwable cause) {
        super("External service unavailable: " + serviceName, cause);
    }
}