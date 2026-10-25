package com.hotel.app.dto.request;

import com.hotel.app.model.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** Cobro en recepcion de una reserva (efectivo, tarjeta, transferencia, Yape/Plin). */
@Data
public class ReceptionPaymentRequest {
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal amount;

    @NotNull(message = "El medio de pago es obligatorio")
    private PaymentMethod method;

    @Size(max = 100)
    private String reference;
}
