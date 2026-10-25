package com.hotel.app.dto.request;

import com.hotel.app.model.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Como se devolvio el dinero. CASH sale de la caja abierta como egreso. */
@Data
public class ProcessRefundRequest {
    @NotNull(message = "Indique el medio de devolución")
    private PaymentMethod method;

    @Size(max = 100)
    private String reference;
}
