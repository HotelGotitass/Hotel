package com.hotel.app.dto.request;

import com.hotel.app.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email invalido")
    private String email;

    private String password;

    @NotNull(message = "El rol es obligatorio")
    private Role role;
}
