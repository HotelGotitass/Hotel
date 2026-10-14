package com.hotel.app.controller;

import com.hotel.app.dto.response.DashboardResponse;
import com.hotel.app.model.enums.InvoiceStatus;
import com.hotel.app.model.enums.ReservationStatus;
import com.hotel.app.model.enums.RoomStatus;
import com.hotel.app.repository.InvoiceRepository;
import com.hotel.app.repository.ReservationRepository;
import com.hotel.app.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final InvoiceRepository invoiceRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<DashboardResponse> getDashboardStats() {
        int totalRooms = (int) roomRepository.count();
        int occupiedRooms = (int) roomRepository.findAll().stream()
                .filter(r -> r.getStatus() == RoomStatus.OCCUPIED).count();
        
        int occupancyRate = totalRooms > 0 ? (occupiedRooms * 100 / totalRooms) : 0;

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        BigDecimal todayRevenue = invoiceRepository.findAll().stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PAID)
                .filter(inv -> !inv.getIssueDate().isBefore(startOfDay) && !inv.getIssueDate().isAfter(endOfDay))
                .map(inv -> inv.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeReservations = reservationRepository.findAll().stream()
                .filter(res -> Arrays.asList(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN).contains(res.getStatus()))
                .count();

        long pendingCheckIns = reservationRepository.findAll().stream()
                .filter(res -> Arrays.asList(ReservationStatus.PENDING, ReservationStatus.CONFIRMED).contains(res.getStatus()))
                .filter(res -> res.getCheckInDate().isEqual(today))
                .count();

        return ResponseEntity.ok(DashboardResponse.builder()
                .totalRooms(totalRooms)
                .occupiedRooms(occupiedRooms)
                .occupancyRate(occupancyRate)
                .todayRevenue(todayRevenue)
                .activeReservations(activeReservations)
                .pendingCheckIns(pendingCheckIns)
                .build());
    }
}
