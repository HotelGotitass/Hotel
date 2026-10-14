package com.hotel.app.controller;

import com.hotel.app.dto.request.ExtraServiceRequest;
import com.hotel.app.dto.response.ExtraServiceResponse;
import com.hotel.app.service.ExtraServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ExtraServiceController {

    private final ExtraServiceService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExtraServiceResponse> createService(@Valid @RequestBody ExtraServiceRequest request) {
        return ResponseEntity.ok(service.createService(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExtraServiceResponse> updateService(@PathVariable Long id, @Valid @RequestBody ExtraServiceRequest request) {
        return ResponseEntity.ok(service.updateService(id, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<ExtraServiceResponse>> getAllServices(@RequestParam(required = false, defaultValue = "false") boolean onlyActive) {
        if (onlyActive) {
            return ResponseEntity.ok(service.getActiveServices());
        }
        return ResponseEntity.ok(service.getAllServices());
    }
}
