package com.hotel.app.integration;

import com.hotel.app.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/**
 * Cliente minimo de la API REST de Mercado Pago (Checkout Pro).
 * Docs: https://www.mercadopago.com.pe/developers/es/reference
 */
@Component
@Slf4j
public class MercadoPagoClient {

    private static final String API_URL = "https://api.mercadopago.com";
    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String accessToken;
    private final String webhookSecret;

    public MercadoPagoClient(@Value("${mercadopago.access-token:}") String accessToken,
                             @Value("${mercadopago.webhook-secret:}") String webhookSecret) {
        this.accessToken = accessToken;
        this.webhookSecret = webhookSecret;
        this.restClient = RestClient.builder()
                .baseUrl(API_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
    }

    public Map<String, Object> createPreference(Map<String, Object> body, String idempotencyKey) {
        ensureConfigured();
        try {
            return restClient.post()
                    .uri("/checkout/preferences")
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JSON_MAP);
        } catch (RestClientResponseException e) {
            log.error("Mercado Pago rechazo la preferencia: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("No se pudo iniciar el pago con Mercado Pago. Intente nuevamente.");
        } catch (RestClientException e) {
            log.error("Error de red con Mercado Pago", e);
            throw new BusinessException("Mercado Pago no esta disponible en este momento. Intente nuevamente.");
        }
    }

    /** Consulta el pago en Mercado Pago: es la unica fuente de verdad sobre estado y monto. */
    public Map<String, Object> getPayment(String paymentId) {
        ensureConfigured();
        return restClient.get()
                .uri("/v1/payments/{id}", paymentId)
                .retrieve()
                .body(JSON_MAP);
    }

    public boolean isSignatureCheckEnabled() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    /**
     * Valida el header x-signature ("ts=...,v1=...") segun
     * https://www.mercadopago.com.pe/developers/es/docs/your-integrations/notifications/webhooks
     */
    public boolean isValidSignature(String xSignature, String xRequestId, String dataId) {
        if (xSignature == null || xSignature.isBlank()) {
            return false;
        }
        String ts = null;
        String v1 = null;
        for (String part : xSignature.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            if ("ts".equals(kv[0])) ts = kv[1];
            if ("v1".equals(kv[0])) v1 = kv[1];
        }
        if (ts == null || v1 == null) {
            return false;
        }

        StringBuilder manifest = new StringBuilder();
        if (dataId != null && !dataId.isBlank()) manifest.append("id:").append(dataId.toLowerCase()).append(';');
        if (xRequestId != null && !xRequestId.isBlank()) manifest.append("request-id:").append(xRequestId).append(';');
        manifest.append("ts:").append(ts).append(';');

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(manifest.toString().getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), v1.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("No se pudo calcular la firma del webhook", e);
            return false;
        }
    }

    private void ensureConfigured() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException("La pasarela de pago no esta configurada (MP_ACCESS_TOKEN).");
        }
    }
}
