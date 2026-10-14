package com.hotel.app.repository;

import com.hotel.app.model.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    java.util.List<Payment> findByReservationId(Long reservationId);
}
