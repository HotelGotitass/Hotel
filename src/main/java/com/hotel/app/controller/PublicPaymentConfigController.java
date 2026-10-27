package com.hotel.app.controller;

import com.hotel.app.integration.MercadoPagoClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Configuracion de pagos que necesita el frontend: la Public Key de Mercado Pago
 * (para inicializar el Payment Brick), el porcentaje de anticipo y la ventana de pago
 * (tambien se muestran en Terminos y Condiciones).
 * Nunca exponer aqui el Access Token ni otra credencial.
 */
@RestController
@RequestMapping("/api/v1/public/payments")
@RequiredArgsConstructor
public class PublicPaymentConfigController {

    private final MercadoPagoClient mercadoPagoClient;

    @Value("${booking.deposit-percentage:50}")
    private BigDecimal depositPercentage;

    @Value("${booking.payment-window-minutes:15}")
    private Integer paymentWindowMinutes;

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        String publicKey = mercadoPagoClient.getPublicKey();
        return ResponseEntity.ok(Map.of(
                "publicKey", publicKey == null ? "" : publicKey,
                "depositPercentage", depositPercentage,
                "paymentWindowMinutes", paymentWindowMinutes));
    }
}
