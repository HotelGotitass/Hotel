package com.hotel.app.controller;

import com.hotel.app.dto.request.ConsumptionRequest;
import com.hotel.app.dto.response.ConsumptionResponse;
import com.hotel.app.service.ConsumptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/consumptions")
@RequiredArgsConstructor
public class ConsumptionController {

    private final ConsumptionService consumptionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<ConsumptionResponse> addConsumption(@Valid @RequestBody ConsumptionRequest request) {
        return ResponseEntity.ok(consumptionService.addConsumption(request));
    }

    @GetMapping("/reservation/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<ConsumptionResponse>> getConsumptions(@PathVariable Long reservationId) {
        return ResponseEntity.ok(consumptionService.getConsumptionsByReservation(reservationId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeConsumption(@PathVariable Long id) {
        consumptionService.removeConsumption(id);
        return ResponseEntity.noContent().build();
    }
}
