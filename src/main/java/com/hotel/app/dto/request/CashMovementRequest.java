package com.hotel.app.dto.request;

import com.hotel.app.model.enums.CashMovementType;
import com.hotel.app.model.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** Ingreso o egreso manual (compra de insumos, retiro, ingreso por otros conceptos). */
@Data
public class CashMovementRequest {
    @NotNull(message = "Indique si es ingreso o egreso")
    private CashMovementType type;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal amount;

    @NotBlank(message = "El concepto es obligatorio")
    @Size(max = 255)
    private String concept;

    private PaymentMethod method = PaymentMethod.CASH;
}
