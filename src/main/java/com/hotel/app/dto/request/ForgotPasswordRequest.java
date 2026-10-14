package com.hotel.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "El email es requerido")
    @Email(message = "Debe ser un email válido")
    private String email;
}
