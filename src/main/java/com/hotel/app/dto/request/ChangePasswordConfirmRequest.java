package com.hotel.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordConfirmRequest {
    @NotBlank(message = "El OTP es requerido")
    private String otp;

    @NotBlank(message = "La nueva contraseña es requerida")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "La contraseña debe tener al menos 8 caracteres, una letra y un número")
    private String newPassword;

    public void setOtp(String otp) {
        this.otp = otp != null ? otp.trim() : null;
    }
}
