package com.hotel.app.repository;

import com.hotel.app.model.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    java.util.List<Payment> findByReservationId(Long reservationId);

    boolean existsByReference(String reference);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.reservation.id = :reservationId")
    BigDecimal sumAmountByReservationId(@Param("reservationId") Long reservationId);
}
