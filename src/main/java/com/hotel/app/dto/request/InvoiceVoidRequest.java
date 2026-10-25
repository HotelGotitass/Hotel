package com.hotel.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InvoiceVoidRequest {
    @NotBlank(message = "El motivo de anulacion es obligatorio")
    @Size(max = 255)
    private String reason;
}
