package com.hotel.app.dto.request;

import com.hotel.app.model.enums.ComplaintType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    private String fullName;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String email;

    @NotBlank(message = "El tipo de documento es obligatorio")
    private String documentType;

    @NotBlank(message = "El número de documento es obligatorio")
    private String documentNumber;

    private String phone;

    @NotNull(message = "El tipo de reclamación (RECLAMO o QUEJA) es obligatorio")
    private ComplaintType complaintType;

    @NotBlank(message = "El bien o servicio contratado es obligatorio")
    private String serviceType;

    @NotBlank(message = "El detalle de la reclamación es obligatorio")
    private String detail;

    private String consumerRequest;
}
