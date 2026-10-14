package com.hotel.app.controller;

import com.hotel.app.dto.request.ComplaintRequest;
import com.hotel.app.dto.response.ComplaintResponse;
import com.hotel.app.model.enums.ComplaintStatus;
import com.hotel.app.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    public ResponseEntity<ComplaintResponse> createComplaint(@Valid @RequestBody ComplaintRequest request) {
        ComplaintResponse created = complaintService.createComplaint(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<ComplaintResponse>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusBody) {
        String statusStr = statusBody.get("status");
        ComplaintStatus status = ComplaintStatus.valueOf(statusStr.toUpperCase());
        return ResponseEntity.ok(complaintService.updateStatus(id, status));
    }
}
