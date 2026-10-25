package com.hotel.app.controller;

import com.hotel.app.dto.response.RefundResponse;
import com.hotel.app.model.entity.Refund;
import com.hotel.app.model.enums.RefundStatus;
import com.hotel.app.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.hotel.app.security.UserDetailsImpl;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

    private final RefundRepository refundRepository;
    private final com.hotel.app.service.CashService cashService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<RefundResponse>> getAllRefunds() {
        return ResponseEntity.ok(refundRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Transactional
    public ResponseEntity<RefundResponse> processRefund(@PathVariable Long id,
                                                        @jakarta.validation.Valid @RequestBody com.hotel.app.dto.request.ProcessRefundRequest request,
                                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new com.hotel.app.exception.ResourceNotFoundException("Reembolso no encontrado"));
                
        if (refund.getStatus() == RefundStatus.PROCESSED) {
            throw new com.hotel.app.exception.BusinessException("El reembolso ya fue procesado");
        }
        com.hotel.app.model.enums.PaymentMethod method = request.getMethod();
        if (method == com.hotel.app.model.enums.PaymentMethod.ONLINE_GATEWAY) {
            throw new com.hotel.app.exception.BusinessException("Medio de devolución no válido");
        }
        // El efectivo sale del cajon: se registra como egreso en la caja abierta (o falla si no hay caja/efectivo)
        if (method == com.hotel.app.model.enums.PaymentMethod.CASH) {
            cashService.registerCashRefund(refund.getReservation().getId(), refund.getAmount(), userDetails.getUsername());
        }
        refund.setRefundMethod(method);
        refund.setRefundReference(request.getReference() != null && !request.getReference().isBlank() ? request.getReference().trim() : null);

        refund.setStatus(RefundStatus.PROCESSED);
        refund.setProcessedBy(userDetails.getUsername());
        refund.setProcessedAt(java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(mapToResponse(refundRepository.save(refund)));
    }

    private RefundResponse mapToResponse(Refund refund) {
        return RefundResponse.builder()
                .id(refund.getId())
                .bookingCode(refund.getReservation().getBookingCode())
                .amount(refund.getAmount())
                .reason(refund.getReason().name())
                .status(refund.getStatus().name())
                .processedBy(refund.getProcessedBy())
                .processedAt(refund.getProcessedAt())
                .method(refund.getRefundMethod() != null ? refund.getRefundMethod().name() : null)
                .reference(refund.getRefundReference())
                .createdAt(refund.getCreatedAt())
                .build();
    }
}
