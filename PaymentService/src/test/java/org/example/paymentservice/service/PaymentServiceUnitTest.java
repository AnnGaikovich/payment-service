package org.example.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.paymentservice.auth.util.SecurityUtils;
import org.example.paymentservice.client.*;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.PaymentStatus;
import org.example.paymentservice.exception.*;
import org.example.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.example.paymentservice.exception.ValidationException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;


@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RandomNumberClient randomNumberClient;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private KafkaPaymentEventProducer kafkaPaymentEventProducer;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;
    private final Long USER_ID = 1L;
    private final Long ORDER_ID = 100L;
    private final Long ADMIN_ID = 999L;
    private final BigDecimal PAYMENT_AMOUNT = new BigDecimal("500.00");

    @BeforeEach
    void setUp() {
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setUserId(USER_ID);
        testPayment.setOrderId(ORDER_ID);
        testPayment.setStatus(PaymentStatus.SUCCESS);
        testPayment.setPaymentAmount(PAYMENT_AMOUNT);
        testPayment.setTimestamp(LocalDateTime.now());
    }

    // ============ CREATE PAYMENT TESTS ============

    @Test
    void createPayment_Success_ReturnsPayment() throws Exception {
        // Arrange
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(orderServiceClient.existsById(ORDER_ID)).thenReturn("true");
        when(userServiceClient.existsById(USER_ID)).thenReturn("true");
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(randomNumberClient.getRandomNumber()).thenReturn(42);
        when(randomNumberClient.isEven(42)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Act
        Payment result = paymentService.createPayment(ORDER_ID, USER_ID, PAYMENT_AMOUNT);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(kafkaPaymentEventProducer, times(1))
                .sendPaymentEvent(eq(testPayment.getId()), eq(ORDER_ID), eq(USER_ID),
                        eq("SUCCESS"), eq(PAYMENT_AMOUNT));
    }

    @Test
    void createPayment_UnauthorizedUser_ThrowsException() {
        // Arrange
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(999L); // Different user

        // Act & Assert
        assertThatThrownBy(() -> paymentService.createPayment(ORDER_ID, USER_ID, PAYMENT_AMOUNT))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("create payment for user");
    }

    @Test
    void createPayment_InvalidAmount_ThrowsValidationException() {
        // Arrange
        BigDecimal negativeAmount = new BigDecimal("-100.00");

        // Настраиваем SecurityUtils, чтобы пройти проверку доступа
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);

        // Act & Assert
        assertThatThrownBy(() -> paymentService.createPayment(ORDER_ID, USER_ID, negativeAmount))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Payment amount must be positive");
    }

    @Test
    void createPayment_PaymentAlreadyExists_ThrowsException() {
        // Arrange
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(orderServiceClient.existsById(ORDER_ID)).thenReturn("true");
        when(userServiceClient.existsById(USER_ID)).thenReturn("true");
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> paymentService.createPayment(ORDER_ID, USER_ID, PAYMENT_AMOUNT))
                .isInstanceOf(PaymentAlreadyExistsException.class);
    }

    @Test
    void createPayment_RandomApiFails_ThrowsExternalServiceException() {
        // Arrange
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(orderServiceClient.existsById(ORDER_ID)).thenReturn("true");
        when(userServiceClient.existsById(USER_ID)).thenReturn("true");
        when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
        when(randomNumberClient.getRandomNumber()).thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.createPayment(ORDER_ID, USER_ID, PAYMENT_AMOUNT))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Random.org");
    }

    // ============ GET PAYMENTS TESTS ============

    @Test
    void getPaymentsByUserId_Success_ReturnsPaymentsList() {
        // Arrange
        List<Payment> payments = Arrays.asList(testPayment);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentRepository.findByUserId(USER_ID)).thenReturn(payments);

        // Act
        List<Payment> result = paymentService.getPaymentsByUserId(USER_ID);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testPayment);
    }

    @Test
    void getPaymentsByUserId_UnauthorizedAccess_ThrowsException() {
        // Arrange
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(999L); // Different user

        // Act & Assert
        assertThatThrownBy(() -> paymentService.getPaymentsByUserId(USER_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void getPaymentsByUserId_AdminAccess_ReturnsPayments() {
        // Arrange
        List<Payment> payments = Arrays.asList(testPayment);
        when(securityUtils.isAdmin()).thenReturn(true);
        when(paymentRepository.findByUserId(USER_ID)).thenReturn(payments);

        // Act
        List<Payment> result = paymentService.getPaymentsByUserId(USER_ID);

        // Assert
        assertThat(result).hasSize(1);
    }

    // ============ TOTAL SUM TESTS ============

    @Test
    void getTotalSumByUserAndDateRange_Success_ReturnsSum() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        BigDecimal expectedSum = new BigDecimal("1500.75");

        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        when(paymentRepository.getTotalSumByUserAndDateRange(USER_ID, startDate, endDate))
                .thenReturn(expectedSum);

        // Act
        BigDecimal result = paymentService.getTotalSumByUserAndDateRange(USER_ID, startDate, endDate);

        // Assert
        assertThat(result).isEqualByComparingTo(expectedSum);
    }

    @Test
    void getTotalSumByUserAndDateRange_InvalidDateRange_ThrowsException() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().minusDays(7);

        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);

        // Act & Assert
        assertThatThrownBy(() ->
                paymentService.getTotalSumByUserAndDateRange(USER_ID, startDate, endDate))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    void getTotalSumByDateRange_AdminAccess_ReturnsSum() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        BigDecimal expectedSum = new BigDecimal("5000.25");

        when(securityUtils.isAdmin()).thenReturn(true);
        when(paymentRepository.getTotalSumByDateRange(startDate, endDate))
                .thenReturn(expectedSum);

        // Act
        BigDecimal result = paymentService.getTotalSumByDateRange(startDate, endDate);

        // Assert
        assertThat(result).isEqualByComparingTo(expectedSum);
    }

    @Test
    void getTotalSumByDateRange_NonAdmin_ThrowsException() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        when(securityUtils.isAdmin()).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> paymentService.getTotalSumByDateRange(startDate, endDate))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    // ============ DELETE PAYMENT TESTS ============

    @Test
    void deletePayment_AdminAccess_Success() {
        // Arrange
        Long paymentId = 1L;
        Payment paymentToDelete = new Payment();
        paymentToDelete.setId(paymentId);
        paymentToDelete.setStatus(PaymentStatus.SUCCESS);

        when(securityUtils.isAdmin()).thenReturn(true);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(paymentToDelete));
        when(paymentRepository.save(any(Payment.class))).thenReturn(paymentToDelete);

        // Act
        paymentService.deletePayment(paymentId);

        // Assert
        assertThat(paymentToDelete.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentRepository, times(1)).save(paymentToDelete);
    }

    @Test
    void deletePayment_NonAdmin_ThrowsException() {
        // Arrange
        Long paymentId = 1L;
        when(securityUtils.isAdmin()).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> paymentService.deletePayment(paymentId))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void deletePayment_PaymentNotFound_ThrowsException() {
        // Arrange
        Long paymentId = 999L;
        when(securityUtils.isAdmin()).thenReturn(true);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> paymentService.deletePayment(paymentId))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    // ============ VALIDATION TESTS ============

    @Test
    void validatePaymentAmount_NullAmount_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() ->
                ReflectionTestUtils.invokeMethod(paymentService, "validatePaymentAmount", (BigDecimal) null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Payment amount cannot be null");
    }

    @Test
    void validatePaymentAmount_AmountTooLarge_ThrowsException() {
        // Arrange
        BigDecimal hugeAmount = new BigDecimal("2000000.00");

        // Act & Assert
        assertThatThrownBy(() ->
                ReflectionTestUtils.invokeMethod(paymentService, "validatePaymentAmount", hugeAmount))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Payment amount cannot exceed");
    }
}