package com.hotel.app.controller;

import com.hotel.app.dto.request.ForgotPasswordRequest;
import com.hotel.app.dto.request.ResetPasswordRequest;
import com.hotel.app.dto.response.ApiResponse;
import com.hotel.app.service.ClientAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/client/auth")
@RequiredArgsConstructor
public class ClientPasswordResetController {

    private final ClientAuthService clientAuthService;

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        clientAuthService.forgotPassword(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Si el correo existe, se ha enviado un OTP.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        clientAuthService.resetPassword(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Contraseña actualizada exitosamente.", null));
    }
}
