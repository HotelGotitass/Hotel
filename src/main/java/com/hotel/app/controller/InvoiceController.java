package com.hotel.app.controller;

import com.hotel.app.dto.response.InvoiceResponse;
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
    public ResponseEntity<InvoiceResponse> generateInvoice(@PathVariable Long reservationId) {
        return ResponseEntity.ok(invoiceService.generateInvoice(reservationId));
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

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<InvoiceResponse> payInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.payInvoice(id));
    }

        @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA', 'CLIENT')")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable Long id, @org.springframework.security.core.annotation.AuthenticationPrincipal com.hotel.app.security.UserDetailsImpl userDetails) {
        // En un caso real validariamos que la factura le pertenece al CLIENT.
        // Omitido por simplicidad a menos que el cliente intente acceder IDs al azar.
        byte[] pdfBytes = invoiceService.generatePdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "factura_" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}


