package com.hotel.app.model.entity;

import com.hotel.app.model.enums.InvoiceDocumentType;
import com.hotel.app.model.enums.InvoiceStatus;
import com.hotel.app.model.enums.SunatStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Varios comprobantes por reserva en el tiempo (p. ej. boleta anulada + factura), uno solo vigente
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private BigDecimal taxAmount;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime issueDate;

    // --- Comprobante electronico (simulado) ---
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 10)
    @Builder.Default
    private InvoiceDocumentType documentType = InvoiceDocumentType.BOLETA;

    @Column(length = 4)
    private String series;

    private Integer correlative;

    @Column(name = "customer_doc_type", length = 20)
    private String customerDocType;

    @Column(name = "customer_doc_number", length = 20)
    private String customerDocNumber;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_address")
    private String customerAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "sunat_status", length = 20)
    private SunatStatus sunatStatus;

    @Column(name = "sunat_message")
    private String sunatMessage;

    @Column(name = "sunat_hash", length = 64)
    private String sunatHash;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "credit_note_series", length = 4)
    private String creditNoteSeries;

    @Column(name = "credit_note_correlative")
    private Integer creditNoteCorrelative;

    public String getNumber() {
        return series == null ? null : series + "-" + String.format("%08d", correlative);
    }

    public String getCreditNoteNumber() {
        return creditNoteSeries == null ? null : creditNoteSeries + "-" + String.format("%08d", creditNoteCorrelative);
    }
}
