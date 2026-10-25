package com.hotel.app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CashMovementResponse {
    private Long id;
    private String type;
    private String method;
    private BigDecimal amount;
    private String concept;
    private String bookingCode;
    private String createdBy;
    private LocalDateTime createdAt;
}
