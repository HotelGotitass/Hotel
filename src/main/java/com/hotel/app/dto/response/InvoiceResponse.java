package com.hotel.app.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceResponse {
    private Long id;
    private Long reservationId;
    private String guestName;
    private String documentNumber;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime issueDate;
}
