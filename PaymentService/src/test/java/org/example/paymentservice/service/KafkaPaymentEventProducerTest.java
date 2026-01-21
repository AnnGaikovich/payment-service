package org.example.paymentservice.service;

import org.example.paymentservice.dto.PaymentEventDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaPaymentEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaPaymentEventProducer kafkaPaymentEventProducer;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<PaymentEventDTO> eventCaptor;

    @Test
    void sendPaymentEvent_Success_SendsToKafka() {
        // Arrange
        Long paymentId = 1L;
        Long orderId = 100L;
        Long userId = 50L;
        String status = "SUCCESS";
        BigDecimal amount = new BigDecimal("500.00");

        // Act
        kafkaPaymentEventProducer.sendPaymentEvent(paymentId, orderId, userId, status, amount);

        // Assert
        verify(kafkaTemplate).send(topicCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("payment-events");

        PaymentEventDTO capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getPaymentId()).isEqualTo(paymentId);
        assertThat(capturedEvent.getOrderId()).isEqualTo(orderId);
        assertThat(capturedEvent.getUserId()).isEqualTo(userId);
        assertThat(capturedEvent.getStatus()).isEqualTo(status);
        assertThat(capturedEvent.getAmount()).isEqualTo(amount);
        assertThat(capturedEvent.getEventType()).isEqualTo("CREATE_PAYMENT");
        assertThat(capturedEvent.getEventId()).isNotNull();
        assertThat(capturedEvent.getTimestamp()).isNotNull();
    }

    @Test
    void sendPaymentEvent_KafkaException_LogsError() {
        // Arrange
        Long paymentId = 1L;
        Long orderId = 100L;
        Long userId = 50L;
        String status = "SUCCESS";
        BigDecimal amount = new BigDecimal("500.00");

        doThrow(new RuntimeException("Kafka error"))
                .when(kafkaTemplate).send(anyString(), any(PaymentEventDTO.class));

        // Act
        kafkaPaymentEventProducer.sendPaymentEvent(paymentId, orderId, userId, status, amount);

        // Assert - No exception should be thrown, just logged
        verify(kafkaTemplate, times(1)).send(anyString(), any(PaymentEventDTO.class));
    }
}