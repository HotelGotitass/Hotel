package com.hotel.app.controller;

import com.hotel.app.dto.request.BrickPaymentRequest;
import com.hotel.app.dto.request.ClientReservationRequest;
import com.hotel.app.dto.request.PaymentPreferenceRequest;
import com.hotel.app.dto.response.BrickPaymentResponse;
import com.hotel.app.dto.response.PaymentPreferenceResponse;
import com.hotel.app.dto.response.ReservationResponse;
import com.hotel.app.service.InvoiceService;
import com.hotel.app.service.PaymentService;
import com.hotel.app.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.hotel.app.security.UserDetailsImpl;
import org.springframework.web.bind.annotation.*;
import com.hotel.app.dto.request.DateModificationRequest;

import java.util.List;

@RestController
@RequestMapping("/api/v1/client/reservations")
@RequiredArgsConstructor
public class ClientReservationController {

    private final ReservationService reservationService;
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ReservationResponse> createWebReservation(
            @Valid @RequestBody ClientReservationRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        return new ResponseEntity<>(reservationService.createWebReservation(request, userDetails.getUsername()), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        return ResponseEntity.ok(reservationService.getClientReservations(userDetails.getUsername()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ReservationResponse> cancelMyReservation(
            @PathVariable Long id, 
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(reservationService.cancelReservationByClient(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/dates")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ReservationResponse> modifyDates(
            @PathVariable Long id, 
            @Valid @RequestBody DateModificationRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(reservationService.modifyReservationDates(id, userDetails.getUsername(), request.getNewCheckIn(), request.getNewCheckOut()));
    }

    @PostMapping("/{id}/payment-preference")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PaymentPreferenceResponse> createPaymentPreference(
            @PathVariable Long id,
            @Valid @RequestBody PaymentPreferenceRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(paymentService.createPreference(id, userDetails.getUsername(), request.getOption()));
    }

    /** Pago con el Payment Brick de Mercado Pago (formulario embebido, inicializado con la Public Key). */
    @PostMapping("/{id}/payments")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<BrickPaymentResponse> payWithBrick(
            @PathVariable Long id,
            @Valid @RequestBody BrickPaymentRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(paymentService.payWithBrick(id, userDetails.getUsername(), request.getOption(), request.getFormData()));
    }

    @GetMapping("/{id}/invoice/pdf")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<byte[]> downloadMyInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        byte[] pdfBytes = invoiceService.generatePdfForClient(id, userDetails.getUsername());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "factura_reserva_" + id + ".pdf");
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
