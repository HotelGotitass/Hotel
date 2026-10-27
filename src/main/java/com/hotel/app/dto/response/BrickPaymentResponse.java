package com.hotel.app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BrickPaymentResponse {
    private String paymentId;
    private String status;
    private String statusDetail;
    private String bookingCode;
    private BigDecimal amount;
}
