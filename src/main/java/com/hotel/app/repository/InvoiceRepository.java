package com.hotel.app.repository;

import com.hotel.app.model.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByReservationId(Long reservationId);
    boolean existsByReservationId(Long reservationId);
}
