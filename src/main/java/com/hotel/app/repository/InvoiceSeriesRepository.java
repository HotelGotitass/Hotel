package com.hotel.app.repository;

import com.hotel.app.model.entity.InvoiceSeries;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceSeriesRepository extends JpaRepository<InvoiceSeries, String> {

    // Bloqueo de fila: dos emisiones simultaneas nunca obtienen el mismo correlativo
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InvoiceSeries s WHERE s.series = :series")
    Optional<InvoiceSeries> findForUpdate(@Param("series") String series);
}
