package com.hotel.app.model.entity;

import com.hotel.app.model.enums.CashSessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Turno de caja de recepcion: apertura con fondo inicial, movimientos y cierre con arqueo. */
@Entity
@Table(name = "cash_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CashSessionStatus status;

    @Column(name = "opened_by", nullable = false)
    private String openedBy;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "opening_amount", nullable = false)
    private BigDecimal openingAmount;

    @Column(name = "closed_by")
    private String closedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "expected_cash")
    private BigDecimal expectedCash;

    @Column(name = "counted_amount")
    private BigDecimal countedAmount;

    private BigDecimal difference;

    @Column(length = 500)
    private String notes;
}
