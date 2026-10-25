package com.hotel.app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CashSessionResponse {
    private Long id;
    private String status;
    private String openedBy;
    private LocalDateTime openedAt;
    private BigDecimal openingAmount;
    private String closedBy;
    private LocalDateTime closedAt;
    private BigDecimal countedAmount;
    private BigDecimal difference;
    private String notes;

    // Resumen (calculado en vivo si la caja esta abierta)
    private BigDecimal expectedCash;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private Map<String, BigDecimal> incomeByMethod;
    private List<CashMovementResponse> movements;
}
