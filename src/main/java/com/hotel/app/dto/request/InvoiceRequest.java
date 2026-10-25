package com.hotel.app.dto.request;

import com.hotel.app.model.enums.InvoiceDocumentType;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Datos del adquiriente. Si se omiten, se usan los del huesped de la reserva (boleta).
 * Para FACTURA son obligatorios RUC, razon social y direccion.
 */
@Data
public class InvoiceRequest {
    private InvoiceDocumentType documentType = InvoiceDocumentType.BOLETA;

    @Size(max = 20)
    private String customerDocType;

    @Size(max = 20)
    private String customerDocNumber;

    @Size(max = 255)
    private String customerName;

    @Size(max = 255)
    private String customerAddress;
}
