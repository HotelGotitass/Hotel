package com.hotel.app.controller;

import com.hotel.app.dto.request.GuestRequest;
import com.hotel.app.dto.response.GuestResponse;
import com.hotel.app.service.GuestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/guests")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<GuestResponse> createGuest(@Valid @RequestBody GuestRequest request) {
        return ResponseEntity.ok(guestService.createGuest(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<GuestResponse> getGuestById(@PathVariable Long id) {
        return ResponseEntity.ok(guestService.getGuestById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<GuestResponse>> searchGuests(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(guestService.searchGuests(query));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<GuestResponse> updateGuest(@PathVariable Long id, @Valid @RequestBody GuestRequest request) {
        return ResponseEntity.ok(guestService.updateGuest(id, request));
    }
}
