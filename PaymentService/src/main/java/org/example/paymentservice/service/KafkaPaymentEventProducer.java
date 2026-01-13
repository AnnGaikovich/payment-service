package org.example.paymentservice.service;

import org.example.paymentservice.dto.PaymentEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaPaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentEvent(Long paymentId, Long orderId, Long userId, String status, BigDecimal amount) {
        PaymentEventDTO event = new PaymentEventDTO();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("CREATE_PAYMENT");
        event.setPaymentId(paymentId);
        event.setOrderId(orderId);
        event.setUserId(userId);
        event.setStatus(status);
        event.setAmount(amount);
        event.setTimestamp(LocalDateTime.now());

        String topic = "payment-events";

        try {
            kafkaTemplate.send(topic, event);
            log.info("Payment event sent: {}", event);
        } catch (Exception e) {
            log.error("Failed to send payment event: {}", event, e);
        }
    }
}