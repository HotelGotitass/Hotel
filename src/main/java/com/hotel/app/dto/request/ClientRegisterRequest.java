package com.hotel.app.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ClientRegisterRequest {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email invalido")
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,}$", message = "La contrasena debe tener al menos 8 caracteres, 1 mayuscula y 1 numero")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    private String lastName;

    @NotBlank(message = "El documento es obligatorio")
    private String documentNumber;

    @NotBlank(message = "El tipo de documento es obligatorio")
    private String documentType;

    private boolean acceptedTerms;
    private boolean acceptedPrivacy;
}
