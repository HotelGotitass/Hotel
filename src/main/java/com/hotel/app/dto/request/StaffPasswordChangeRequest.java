package com.hotel.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Cambio de la propia contrasena (requiere la actual). */
@Data
public class StaffPasswordChangeRequest {
    @NotBlank(message = "Ingrese su contraseña actual")
    private String currentPassword;

    @NotBlank(message = "Ingrese la nueva contraseña")
    @Size(min = 8, max = 72, message = "La nueva contraseña debe tener entre 8 y 72 caracteres")
    private String newPassword;
}
