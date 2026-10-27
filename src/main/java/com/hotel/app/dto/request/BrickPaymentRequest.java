package com.hotel.app.dto.request;

import com.hotel.app.model.enums.PaymentOption;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * Datos que envia el Payment Brick de Mercado Pago en onSubmit (formData).
 * El monto NO se toma de aqui: lo calcula el backend segun la opcion elegida.
 */
@Data
public class BrickPaymentRequest {
    @NotNull(message = "La opcion de pago es obligatoria")
    private PaymentOption option;

    @NotNull(message = "Faltan los datos del formulario de pago")
    private Map<String, Object> formData;
}
