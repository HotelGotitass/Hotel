package com.hotel.app.controller;

import com.hotel.app.dto.request.StaffPasswordChangeRequest;
import com.hotel.app.dto.response.UserResponse;
import com.hotel.app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Cuenta del usuario autenticado (administrador o recepcionista). */
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
public class AccountController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication auth) {
        return ResponseEntity.ok(userService.getByEmail(auth.getName()));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody StaffPasswordChangeRequest request, Authentication auth) {
        userService.changeOwnPassword(auth.getName(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
