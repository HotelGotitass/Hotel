package com.hotel.app.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ReservationResponse {
    private Long id;
    private String bookingCode;
    private String originChannel;
    private Integer guests;
    private Long guestId;
    private String guestName;
    private Long categoryId;
    private String categoryName;
    private Long roomId;
    private String roomNumber;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String status;
    private BigDecimal totalPrice;
    private String notes;
    private LocalDateTime createdAt;
    private String cancelledBy;
    private String cancellationReason;
    private Integer modificationCount;
    private LocalDateTime expiresAt;
    // Segundos restantes para pagar; evita depender de la zona horaria del navegador vs. servidor
    private Long paymentExpiresInSeconds;
    private BigDecimal amountPaid;
    private BigDecimal depositAmount;
    private BigDecimal balanceDue;
}


