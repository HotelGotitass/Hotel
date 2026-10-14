package com.hotel.app.repository;

import com.hotel.app.model.entity.Consumption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConsumptionRepository extends JpaRepository<Consumption, Long> {
    List<Consumption> findByReservationId(Long reservationId);
}
