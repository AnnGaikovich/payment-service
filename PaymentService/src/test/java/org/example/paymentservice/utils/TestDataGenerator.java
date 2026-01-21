package org.example.paymentservice.utils;

import org.example.paymentservice.dto.PaymentRequestDTO;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TestDataGenerator {

    public static Payment createTestPayment(Long id, Long userId, Long orderId) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setUserId(userId);
        payment.setOrderId(orderId);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentAmount(new BigDecimal("100.50"));
        payment.setTimestamp(LocalDateTime.now());
        return payment;
    }

    public static PaymentRequestDTO createPaymentRequestDTO(Long orderId, Long userId) {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setOrderId(orderId);
        request.setUserId(userId);
        request.setPaymentAmount(new BigDecimal("250.75"));
        return request;
    }
}