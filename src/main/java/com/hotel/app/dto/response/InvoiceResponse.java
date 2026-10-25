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
    private String bookingCode;
    private String guestName;
    private String documentNumber;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime issueDate;

    // Comprobante electronico (simulado)
    private String documentType;
    private String number;
    private String customerDocType;
    private String customerDocNumber;
    private String customerName;
    private String customerAddress;
    private String sunatStatus;
    private String sunatMessage;
    private String sunatHash;
    private String voidReason;
    private LocalDateTime voidedAt;
    private String creditNoteNumber;
}
