package com.hotel.app.controller;

import com.hotel.app.dto.request.ChangePasswordConfirmRequest;
import com.hotel.app.dto.request.ClientProfileUpdateRequest;
import com.hotel.app.dto.response.ApiResponse;
import com.hotel.app.security.UserDetailsImpl;
import com.hotel.app.service.ClientProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/client/profile")
@RequiredArgsConstructor
public class ClientProfileController {

    private final ClientProfileService clientProfileService;

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ClientProfileUpdateRequest request) {
        
        clientProfileService.updateProfile(userDetails.getId(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Perfil actualizado exitosamente.", null));
    }

    @PostMapping("/password-request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordChange(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        clientProfileService.requestPasswordChange(userDetails.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Se ha enviado un OTP a su correo.", null));
    }

    @PostMapping("/password-change")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ChangePasswordConfirmRequest request) {
        
        clientProfileService.changePassword(userDetails.getId(), request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Contraseña actualizada exitosamente.", null));
    }
}
