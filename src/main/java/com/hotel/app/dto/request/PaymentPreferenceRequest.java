package com.hotel.app.dto.request;

import com.hotel.app.model.enums.PaymentOption;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentPreferenceRequest {
    @NotNull(message = "La opcion de pago es obligatoria")
    private PaymentOption option;
}
