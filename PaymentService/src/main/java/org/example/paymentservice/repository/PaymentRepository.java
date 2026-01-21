package org.example.paymentservice.repository;

import org.example.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 1. Get Payments by user_id
    List<Payment> findByUserId(Long userId);

    // 2. Get total sum of payments for date range for current user
    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM Payment p " +
            "WHERE p.userId = :userId " +
            "AND p.timestamp BETWEEN :startDate AND :endDate")
    BigDecimal getTotalSumByUserAndDateRange(@Param("userId") Long userId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    // 3. Get total sum of payments for date range for all users (for admin)
    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM Payment p " +
            "WHERE p.timestamp BETWEEN :startDate AND :endDate")
    BigDecimal getTotalSumByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);
    // 7. Проверка существования платежа для заказа (для идемпотентности)
    boolean existsByOrderId(Long orderId);
}