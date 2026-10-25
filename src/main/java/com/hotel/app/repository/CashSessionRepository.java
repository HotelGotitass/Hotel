package com.hotel.app.repository;

import com.hotel.app.model.entity.CashSession;
import com.hotel.app.model.enums.CashSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CashSessionRepository extends JpaRepository<CashSession, Long> {

    Optional<CashSession> findFirstByStatus(CashSessionStatus status);

    // Bloquea la caja abierta mientras se registra un movimiento o se cierra
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CashSession s WHERE s.status = :status")
    Optional<CashSession> findByStatusForUpdate(@Param("status") CashSessionStatus status);

    List<CashSession> findTop30ByStatusOrderByOpenedAtDesc(CashSessionStatus status);
}
