package com.hotel.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "El email es requerido")
    @Email(message = "Debe ser un email válido")
    private String email;

    @NotBlank(message = "El OTP es requerido")
    private String otp;

    @NotBlank(message = "La nueva contraseña es requerida")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "La contraseña debe tener al menos 8 caracteres, una letra y un número")
    private String newPassword;

    public void setOtp(String otp) {
        this.otp = otp != null ? otp.trim() : null;
    }
}
