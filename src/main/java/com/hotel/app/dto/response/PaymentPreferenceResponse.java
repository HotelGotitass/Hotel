package com.hotel.app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentPreferenceResponse {
    private String preferenceId;
    private String initPoint;
    private String sandboxInitPoint;
    private String bookingCode;
    private BigDecimal amount;
}
