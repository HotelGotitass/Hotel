package com.hotel.app.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CloseCashRequest {
    @NotNull(message = "Ingrese el efectivo contado")
    @DecimalMin(value = "0.00", message = "El efectivo contado no puede ser negativo")
    private BigDecimal countedAmount;

    @Size(max = 500)
    private String notes;
}
