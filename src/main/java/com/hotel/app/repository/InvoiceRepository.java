package com.hotel.app.repository;

import com.hotel.app.model.entity.Invoice;
import com.hotel.app.model.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    // Comprobante vigente (no anulado) de la reserva
    Optional<Invoice> findFirstByReservationIdAndStatusNotOrderByIdDesc(Long reservationId, InvoiceStatus status);
    boolean existsByReservationIdAndStatusNot(Long reservationId, InvoiceStatus status);
    List<Invoice> findAllByOrderByIdDesc();
}
