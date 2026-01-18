package org.example.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.paymentservice.auth.util.SecurityUtils;
import org.example.paymentservice.client.*;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.PaymentStatus;
import org.example.paymentservice.exception.*;
import org.example.paymentservice.repository.PaymentRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RandomNumberClient randomNumberClient;
    private final SecurityUtils securityUtils;
    private final UserServiceClient userServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final KafkaPaymentEventProducer kafkaPaymentEventProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Константы для валидации
    private static final BigDecimal MAX_PAYMENT_AMOUNT = new BigDecimal("1000000");

    // 1. Create Payment с определением статуса через внешний API
    @Transactional
    public Payment createPayment(Long orderId, Long userId, BigDecimal paymentAmount) {
        log.info("Creating payment for orderId: {}, userId: {}, amount: {}", orderId, userId, paymentAmount);

        validateUserAccess(userId, "create payment for user");
        validatePaymentAmount(paymentAmount);

        validateOrderExists(orderId);
        validateUserExists(userId);

        // Идемпотентность: проверяем, не существует ли уже платеж для этого заказа
        if (paymentRepository.existsByOrderId(orderId)) {
            throw new PaymentAlreadyExistsException(orderId);
        }

        // Получаем случайное число от внешнего API
        int randomNumber;
        try {
            randomNumber = randomNumberClient.getRandomNumber();
        } catch (Exception e) {
            log.error("Failed to get random number from external API", e);
            throw new ExternalServiceException("Random.org", e);
        }
        log.info("Got random number from API: {}", randomNumber);

        // Определяем статус платежа на основе случайного числа
        PaymentStatus status = randomNumberClient.isEven(randomNumber) ?
                PaymentStatus.SUCCESS : PaymentStatus.FAILED;

        log.info("Payment status determined as: {} (number was {})", status, randomNumber);

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setPaymentAmount(paymentAmount);
        payment.setStatus(status);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created: id={}, orderId={}, userId={}, status={}",
                savedPayment.getId(), orderId, userId, status);

        // Отправка события в Kafka
        try {
            kafkaPaymentEventProducer.sendPaymentEvent(
                    savedPayment.getId(),
                    orderId,
                    userId,
                    status.toString(),
                    paymentAmount
            );
        } catch (Exception e) {
            // Логируем ошибку, но не прерываем выполнение
            log.error("Failed to send payment event for paymentId: {}", savedPayment.getId(), e);
        }

        return savedPayment;
    }

    // 3. Get Payments by user_id
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByUserId(Long userId) {
        validateUserAccess(userId, "access payments");
        return paymentRepository.findByUserId(userId);
    }

    // 4. Get total sum of payments for date range for specific user
    @Transactional(readOnly = true)
    public BigDecimal getTotalSumByUserAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        validateUserAccess(userId, "access payment statistics");
        validateDateRange(startDate, endDate);
        return paymentRepository.getTotalSumByUserAndDateRange(userId, startDate, endDate);
    }

    // 5. Get total sum of payments for date range for all users (admin only)
    @Transactional(readOnly = true)
    public BigDecimal getTotalSumByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        validateAdminAccess("access all payment statistics");
        validateDateRange(startDate, endDate);
        return paymentRepository.getTotalSumByDateRange(startDate, endDate);
    }

    // 7. Delete Payment (admin only) - мягкое удаление
    @Transactional
    public void deletePayment(Long paymentId) {
        validateAdminAccess("delete payment");

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        // Мягкое удаление - пометка статусом CANCELLED
        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);

        log.info("Payment {} marked as CANCELLED", paymentId);
    }

    // === ВАЛИДАЦИОННЫЕ МЕТОДЫ С ВНЕШНИМИ СЕРВИСАМИ ===

    private void validateUserExists(Long userId) {
        try {
            String userResponse = userServiceClient.existsById(userId);

            // Убираем пробелы и парсим boolean
            userResponse = userResponse.trim();
            Boolean userExists;

            if (userResponse.equalsIgnoreCase("true") || userResponse.equals("1")) {
                userExists = true;
            } else if (userResponse.equalsIgnoreCase("false") || userResponse.equals("0")) {
                userExists = false;
            } else {
                // Пробуем распарсить как JSON boolean
                try {
                    userExists = objectMapper.readValue(userResponse, Boolean.class);
                } catch (Exception e) {
                    log.error("Failed to parse user exists response: {}", userResponse);
                    throw new ValidationException("Invalid response from UserService");
                }
            }

            if (userExists == null || !userExists) {
                throw new ValidationException("User with id " + userId + " does not exist");
            }
            log.debug("User {} exists validation passed", userId);
        } catch (FeignException.NotFound e) {
            throw new ValidationException("User with id " + userId + " does not exist");
        } catch (FeignException e) {
            log.error("Error while calling UserService: {}", e.getMessage());
            throw new ExternalServiceException("UserService", e);
        } catch (Exception e) {
            log.error("Unexpected error while validating user: {}", e.getMessage());
            throw new ValidationException("Failed to validate user existence");
        }
    }

    private void validateOrderExists(Long orderId) {
        try {
            String orderResponse = orderServiceClient.existsById(orderId);

            // Убираем пробелы и парсим boolean
            orderResponse = orderResponse.trim();
            Boolean orderExists;

            if (orderResponse.equalsIgnoreCase("true") || orderResponse.equals("1")) {
                orderExists = true;
            } else if (orderResponse.equalsIgnoreCase("false") || orderResponse.equals("0")) {
                orderExists = false;
            } else {
                // Пробуем распарсить как JSON boolean
                try {
                    orderExists = objectMapper.readValue(orderResponse, Boolean.class);
                } catch (Exception e) {
                    log.error("Failed to parse order exists response: {}", orderResponse); // ИСПРАВЛЕНО: было "user exists"
                    throw new ValidationException("Invalid response from OrderService"); // ИСПРАВЛЕНО: было "UserService"
                }
            }

            // ФИКС: проверяем и на null, и на false
            if (orderExists == null || !orderExists) {
                throw new ValidationException("Order with id " + orderId + " does not exist");
            }

            log.debug("Order {} exists validation passed", orderId);
        } catch (FeignException.NotFound e) {
            throw new ValidationException("Order with id " + orderId + " does not exist");
        } catch (FeignException e) {
            log.error("Error while calling OrderService: {}", e.getMessage());
            throw new ExternalServiceException("OrderService", e);
        } catch (Exception e) {
            log.error("Unexpected error while validating order: {}", e.getMessage());
            throw new ValidationException("Failed to validate order existence");
        }
    }

    // === ОСНОВНЫЕ ВАЛИДАЦИОННЫЕ МЕТОДЫ ===

    private void validateUserAccess(Long targetUserId, String operation) {
        if (!securityUtils.isAdmin() && !targetUserId.equals(securityUtils.getCurrentUserId())) {
            throw new UnauthorizedAccessException(operation, targetUserId);
        }
    }

    private void validateAdminAccess(String operation) {
        if (!securityUtils.isAdmin()) {
            throw new UnauthorizedAccessException(operation);
        }
    }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException();
        }
    }

    private void validatePaymentAmount(BigDecimal amount) {
        if (amount == null) {
            throw new ValidationException("Payment amount cannot be null");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Payment amount must be positive");
        }

        if (amount.compareTo(MAX_PAYMENT_AMOUNT) > 0) {
            throw new ValidationException("Payment amount cannot exceed " + MAX_PAYMENT_AMOUNT);
        }
    }

}