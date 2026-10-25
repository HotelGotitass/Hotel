package com.hotel.app.repository;

import com.hotel.app.model.entity.CashMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {
    List<CashMovement> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
