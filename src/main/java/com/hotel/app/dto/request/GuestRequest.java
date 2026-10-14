package com.hotel.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GuestRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String firstName;
    
    @NotBlank(message = "El apellido es obligatorio")
    private String lastName;
    
    @NotBlank(message = "El tipo de documento es obligatorio")
    private String documentType;
    
    @NotBlank(message = "El numero de documento es obligatorio")
    private String documentNumber;
    
    private String email;
    private String phone;
    private String address;
}
