package com.hotel.app.repository;

import com.hotel.app.model.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByReservationId(Long reservationId);
}
