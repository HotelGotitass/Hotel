package com.hotel.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** El administrador asigna una nueva contrasena a un empleado. */
@Data
public class AdminPasswordResetRequest {
    @NotBlank(message = "Ingrese la nueva contraseña")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String newPassword;
}
