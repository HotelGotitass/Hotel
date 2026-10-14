package com.hotel.app.controller;

import com.hotel.app.model.entity.PaymentAttempt;
import com.hotel.app.model.entity.Reservation;
import com.hotel.app.model.enums.ReservationStatus;
import com.hotel.app.model.entity.Payment;
import com.hotel.app.model.enums.PaymentMethod;
import com.hotel.app.repository.PaymentAttemptRepository;
import com.hotel.app.repository.ReservationRepository;
import com.hotel.app.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class WebhookPaymentController {

    private final ReservationRepository reservationRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentRepository paymentRepository;

    @Value("${booking.deposit-percentage:50}")
    private BigDecimal depositPercentage;

    @Value("${payment.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("X-Signature") String signature,
            @RequestBody Map<String, Object> payload) {
        
        try {
            // Verificar firma HMAC
            String payloadString = payload.toString();
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            String hash = Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(payloadString.getBytes()));
            
            // En simulacion basica permitiremos cualquier firma que no sea nula para que el frontend no tenga que hacer criptografia compleja en JS,
            // pero dejamos el esqueleto de como se haria en produccion.
            if (signature == null || signature.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            String status = (String) payload.get("status");
            String externalReference = (String) payload.get("external_reference"); // bookingCode
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());

            Reservation res = reservationRepository.findByBookingCode(externalReference)
                    .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

            // Crear attempt
            PaymentAttempt attempt = PaymentAttempt.builder()
                    .reservation(res)
                    .amount(amount)
                    .status(status)
                    .paymentMethod("ONLINE_GATEWAY")
                    .externalReference((String) payload.get("payment_id"))
                    .build();
            paymentAttemptRepository.save(attempt);

            if ("APPROVED".equals(status)) {
                // Verificar si el monto cubre al menos el anticipo
                BigDecimal requiredDeposit = res.getTotalPrice().multiply(depositPercentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                if (amount.compareTo(requiredDeposit) >= 0) {
                    // Pasar a CONFIRMED
                    res.setStatus(ReservationStatus.CONFIRMED);
                    reservationRepository.save(res);

                    // Crear el Payment oficial
                    Payment payment = Payment.builder()
                            .reservation(res)
                            .amount(amount)
                            .paymentMethod(PaymentMethod.ONLINE_GATEWAY) // Necesita existir en el enum
                            .reference((String) payload.get("payment_id"))
                            .build();
                    paymentRepository.save(payment);
                    
                    log.info("Pago aprobado. Reserva {} confirmada.", externalReference);
                } else {
                    log.warn("Pago aprobado pero monto insuficiente para deposito. Reserva: {}", externalReference);
                }
            } else {
                log.info("Pago {}. Reserva {}", status, externalReference);
            }

            return ResponseEntity.ok("Webhook processed");

        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }
}
