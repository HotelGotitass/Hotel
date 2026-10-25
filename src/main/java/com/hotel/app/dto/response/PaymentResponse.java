package com.hotel.app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private BigDecimal amount;
    private String method;
    private String reference;
    private LocalDateTime createdAt;
    // Estado de la reserva tras el cobro
    private String reservationStatus;
    private BigDecimal amountPaid;
    private BigDecimal balanceDue;
}
