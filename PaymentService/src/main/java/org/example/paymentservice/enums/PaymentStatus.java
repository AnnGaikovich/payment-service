package org.example.paymentservice.enums;

public enum PaymentStatus {
    PENDING,        // Платеж создан, но еще не обработан
    PROCESSING,     // Платеж в процессе обработки
    SUCCESS,        // Платеж успешно завершен
    FAILED,         // Платеж не прошел
    REFUNDED,       // Средства возвращены
    CANCELLED,      // Платеж отменен
    PARTIAL_REFUND, // Частичный возврат
    EXPIRED         // Платеж просрочен
}