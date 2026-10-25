package com.hotel.app.controller;

import com.hotel.app.dto.request.ReservationRequest;
import com.hotel.app.dto.response.CategoryAvailabilityResponse;
import com.hotel.app.dto.response.ReservationResponse;
import com.hotel.app.model.enums.RefundReason;
import com.hotel.app.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final com.hotel.app.service.CashService cashService;

    // Public Endpoint
    @GetMapping("/rooms/search")
    public ResponseEntity<List<CategoryAvailabilityResponse>> searchAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return ResponseEntity.ok(reservationService.searchAvailability(checkIn, checkOut));
    }

    // Protected Endpoints
    @PostMapping("/reservations")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.createReservation(request));
    }

    @GetMapping("/reservations")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<ReservationResponse>> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @PatchMapping("/reservations/{id}/checkin")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<ReservationResponse> checkIn(@PathVariable Long id, @RequestParam Long roomId) {
        return ResponseEntity.ok(reservationService.assignRoomAndCheckIn(id, roomId));
    }

    @PatchMapping("/reservations/{id}/checkout")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<ReservationResponse> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.checkOut(id));
    }

    @PatchMapping("/reservations/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable Long id, @RequestParam(defaultValue="HOTEL_CANCELLATION") RefundReason reason) {
        return ResponseEntity.ok(reservationService.cancelReservationByAdmin(id, reason));
    }

    // Cobro en recepcion (requiere caja abierta)
    @PostMapping("/reservations/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<com.hotel.app.dto.response.PaymentResponse> registerPayment(
            @PathVariable Long id,
            @Valid @RequestBody com.hotel.app.dto.request.ReceptionPaymentRequest request,
            org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(cashService.registerReservationPayment(id, request.getAmount(), request.getMethod(),
                request.getReference(), auth.getName()));
    }

    @GetMapping("/reservations/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<com.hotel.app.dto.response.PaymentResponse>> getPayments(@PathVariable Long id) {
        return ResponseEntity.ok(cashService.getReservationPayments(id));
    }

    @GetMapping("/reservations/code/{bookingCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<ReservationResponse> getReservationByCode(@PathVariable String bookingCode) {
        return ResponseEntity.ok(reservationService.getReservationByCode(bookingCode));
    }
}
