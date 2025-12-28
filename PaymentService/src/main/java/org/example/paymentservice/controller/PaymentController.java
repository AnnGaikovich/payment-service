package org.example.paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.paymentservice.dto.PaymentRequestDTO;
import org.example.paymentservice.dto.DateRangeRequestDTO;
import org.example.paymentservice.dto.PaymentResponseDTO;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.mapper.PaymentMapper;
import org.example.paymentservice.service.PaymentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    // 1. Create Payment
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PaymentResponseDTO> createPayment(@Valid @RequestBody PaymentRequestDTO request) {
        Payment payment = paymentService.createPayment(
                request.getOrderId(),
                request.getUserId(),
                request.getPaymentAmount()
        );
        PaymentResponseDTO response = paymentMapper.toResponse(payment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 3. Get Payments by user_id
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentsByUserId(@PathVariable Long userId) {
        List<Payment> payments = paymentService.getPaymentsByUserId(userId);
        List<PaymentResponseDTO> responses = payments.stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // 4. Get total sum of payments for date range for current user
    @PostMapping("/user/{userId}/total")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<BigDecimal> getUserTotalSum(
            @PathVariable Long userId,
            @Valid @RequestBody DateRangeRequestDTO dateRange) {

        BigDecimal total = paymentService.getTotalSumByUserAndDateRange(
                userId,
                dateRange.getStartDate(),
                dateRange.getEndDate()
        );
        return ResponseEntity.ok(total);
    }

    // 5. Get total sum of payments for date range for all users (admin only)
    @PostMapping("/admin/total")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BigDecimal> getTotalSum(@Valid @RequestBody DateRangeRequestDTO dateRange) {
        BigDecimal total = paymentService.getTotalSumByDateRange(
                dateRange.getStartDate(),
                dateRange.getEndDate()
        );
        return ResponseEntity.ok(total);
    }

    // 7. Delete Payment (admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }

}