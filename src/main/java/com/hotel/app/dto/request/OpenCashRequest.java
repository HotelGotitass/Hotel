package com.hotel.app.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OpenCashRequest {
    @NotNull(message = "El fondo inicial es obligatorio")
    @DecimalMin(value = "0.00", message = "El fondo inicial no puede ser negativo")
    private BigDecimal openingAmount;
}
