package com.hotel.app.controller;

import com.hotel.app.dto.request.InvoiceRequest;
import com.hotel.app.dto.request.InvoiceVoidRequest;
import com.hotel.app.dto.response.InvoiceResponse;
import jakarta.validation.Valid;
import com.hotel.app.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/reservation/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<InvoiceResponse> generateInvoice(@PathVariable Long reservationId,
                                                           @Valid @RequestBody(required = false) InvoiceRequest request) {
        return ResponseEntity.ok(invoiceService.generateInvoice(reservationId, request));
    }

    @GetMapping("/reservation/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<InvoiceResponse> getInvoiceByReservationId(@PathVariable Long reservationId) {
        return ResponseEntity.ok(invoiceService.getInvoiceByReservationId(reservationId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    // Anulacion con nota de credito (simulada)
    @PatchMapping("/{id}/void")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<InvoiceResponse> voidInvoice(@PathVariable Long id, @Valid @RequestBody InvoiceVoidRequest request) {
        return ResponseEntity.ok(invoiceService.voidInvoice(id, request.getReason()));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<InvoiceResponse> payInvoice(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "CASH") com.hotel.app.model.enums.PaymentMethod method,
                                                      org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(invoiceService.payInvoice(id, method, auth.getName()));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable Long id) {
        // Los clientes descargan su factura por /client/reservations/{id}/invoice/pdf (valida propiedad)
        byte[] pdfBytes = invoiceService.generatePdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "factura_" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}


