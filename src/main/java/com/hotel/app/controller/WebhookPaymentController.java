package com.hotel.app.controller;

import com.hotel.app.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Recibe las notificaciones de Mercado Pago. El cuerpo solo trae el id del pago;
 * el estado y el monto reales se consultan a la API de Mercado Pago en PaymentService.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class WebhookPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestParam(value = "data.id", required = false) String dataIdParam,
            @RequestParam(value = "type", required = false) String typeParam,
            @RequestBody(required = false) Map<String, Object> body) {

        String type = typeParam != null ? typeParam : body != null ? (String) body.get("type") : null;
        String dataId = dataIdParam;
        if (dataId == null && body != null && body.get("data") instanceof Map<?, ?> data && data.get("id") != null) {
            dataId = String.valueOf(data.get("id"));
        }

        if (!"payment".equals(type) || dataId == null) {
            // merchant_order, pruebas de configuracion, etc.: confirmar recepcion y no hacer nada
            return ResponseEntity.ok().build();
        }

        try {
            paymentService.processPaymentNotification(dataId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Un 5xx hace que Mercado Pago reintente la notificacion
            log.error("Error procesando el webhook del pago {}", dataId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
