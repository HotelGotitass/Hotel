package com.hotel.app.controller;

import com.hotel.app.dto.request.ClientReservationRequest;
import com.hotel.app.dto.response.ReservationResponse;
import com.hotel.app.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.hotel.app.security.UserDetailsImpl;
import org.springframework.web.bind.annotation.*;
import com.hotel.app.dto.request.DateModificationRequest;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/client/reservations")
@RequiredArgsConstructor
public class ClientReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ReservationResponse> createWebReservation(
            @Valid @RequestBody ClientReservationRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        return new ResponseEntity<>(reservationService.createWebReservation(request, userDetails.getUsername()), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        return ResponseEntity.ok(reservationService.getClientReservations(userDetails.getUsername()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ReservationResponse> cancelMyReservation(
            @PathVariable Long id, 
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(reservationService.cancelReservationByClient(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/dates")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ReservationResponse> modifyDates(
            @PathVariable Long id, 
            @Valid @RequestBody DateModificationRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(reservationService.modifyReservationDates(id, userDetails.getUsername(), request.getNewCheckIn(), request.getNewCheckOut()));
    }
}
