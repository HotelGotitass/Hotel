package com.hotel.app.controller;

import com.hotel.app.dto.request.CashMovementRequest;
import com.hotel.app.dto.request.CloseCashRequest;
import com.hotel.app.dto.request.OpenCashRequest;
import com.hotel.app.dto.response.CashSessionResponse;
import com.hotel.app.service.CashService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cash")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
public class CashController {

    private final CashService cashService;

    /** Caja abierta actual; 204 si no hay ninguna. */
    @GetMapping("/current")
    public ResponseEntity<CashSessionResponse> current() {
        return cashService.getCurrentSession()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/open")
    public ResponseEntity<CashSessionResponse> open(@Valid @RequestBody OpenCashRequest request, Authentication auth) {
        return new ResponseEntity<>(cashService.openSession(request.getOpeningAmount(), auth.getName()), HttpStatus.CREATED);
    }

    @PostMapping("/movements")
    public ResponseEntity<CashSessionResponse> addMovement(@Valid @RequestBody CashMovementRequest request, Authentication auth) {
        return ResponseEntity.ok(cashService.addMovement(request, auth.getName()));
    }

    @PostMapping("/close")
    public ResponseEntity<CashSessionResponse> close(@Valid @RequestBody CloseCashRequest request, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(cashService.closeSession(request, auth.getName(), isAdmin));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<CashSessionResponse>> history() {
        return ResponseEntity.ok(cashService.getHistory());
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<CashSessionResponse> session(@PathVariable Long id) {
        return ResponseEntity.ok(cashService.getSession(id));
    }
}
