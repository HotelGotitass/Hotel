package com.hotel.app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RefundResponse {
    private Long id;
    private String bookingCode;
    private BigDecimal amount;
    private String reason;
    private String status;
    private String processedBy;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
