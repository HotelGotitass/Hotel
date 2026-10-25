package com.hotel.app.service;

import com.hotel.app.model.entity.Reservation;
import com.hotel.app.model.enums.CancelledBy;
import com.hotel.app.model.enums.ReservationStatus;
import com.hotel.app.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationExpirationTask {

    private final ReservationRepository reservationRepository;

    // Ejecutar cada minuto
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirePendingReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredReservations = reservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, now);

        if (!expiredReservations.isEmpty()) {
            log.info("Encontradas {} reservas PENDING expiradas.", expiredReservations.size());
            for (Reservation res : expiredReservations) {
                res.setStatus(ReservationStatus.CANCELLED);
                res.setCancelledBy(CancelledBy.SYSTEM);
                res.setCancellationReason("No se recibio el pago dentro del plazo");
                res.setNotes((res.getNotes() != null ? res.getNotes() + "\n" : "") + "Cancelada automaticamente por falta de pago (expirada).");
                log.info("Reserva {} cancelada por expiracion.", res.getBookingCode());
            }
            reservationRepository.saveAll(expiredReservations);
        }
    }
}
