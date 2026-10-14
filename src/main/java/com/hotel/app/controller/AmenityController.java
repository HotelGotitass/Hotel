package com.hotel.app.controller;

import com.hotel.app.dto.request.AmenityRequest;
import com.hotel.app.dto.response.AmenityResponse;
import com.hotel.app.service.AmenityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/amenities")
@RequiredArgsConstructor
public class AmenityController {

    private final AmenityService amenityService;

    @GetMapping
    public ResponseEntity<List<AmenityResponse>> getAllAmenities() {
        return ResponseEntity.ok(amenityService.getAllAmenities());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AmenityResponse> createAmenity(@Valid @RequestBody AmenityRequest request) {
        return ResponseEntity.ok(amenityService.createAmenity(request));
    }
}
