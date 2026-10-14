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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    public ResponseEntity<List<RefundResponse>> getAllRefunds() {
        return ResponseEntity.ok(refundRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList()));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPCIONISTA')")
    @Transactional
    public ResponseEntity<RefundResponse> processRefund(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reembolso no encontrado"));
                
        if (refund.getStatus() == RefundStatus.PROCESSED) {
            throw new RuntimeException("El reembolso ya fue procesado");
        }

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
                .createdAt(refund.getCreatedAt())
                .build();
    }
}
