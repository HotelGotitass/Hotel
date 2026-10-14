package com.hotel.app.job;

import com.hotel.app.model.entity.Reservation;
import com.hotel.app.model.enums.ReservationStatus;
import com.hotel.app.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationCronJob {

    private final ReservationRepository reservationRepository;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cancelNoShows() {
        log.info("Iniciando tarea programada: Cancelacion automatica de No-Shows...");
        
        LocalDate today = LocalDate.now();
        List<Reservation> noShows = reservationRepository.findAll().stream()
                .filter(res -> (res.getStatus() == ReservationStatus.CONFIRMED || res.getStatus() == ReservationStatus.PENDING))
                .filter(res -> res.getCheckInDate().isBefore(today))
                .toList();

        if (noShows.isEmpty()) {
            log.info("No se encontraron reservas No-Show para cancelar.");
            return;
        }

        for (Reservation res : noShows) {
            res.setStatus(ReservationStatus.CANCELLED);
            String prefix = res.getNotes() != null ? res.getNotes() + " | " : "";
            res.setNotes(prefix + "Cancelada automatica por el sistema (No-Show).");
        }

        reservationRepository.saveAll(noShows);
        log.info("Tarea completada. {} reservas han sido canceladas.", noShows.size());
    }
}
