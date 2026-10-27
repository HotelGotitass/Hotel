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

import java.util.Map;

/**
 * Cliente minimo de la API REST de Mercado Pago (Checkout Pro y Payment Brick).
 * Docs: https://www.mercadopago.com.pe/developers/es/reference
 */
@Component
@Slf4j
public class MercadoPagoClient {

    private static final String API_URL = "https://api.mercadopago.com";
    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String accessToken;
    private final String publicKey;

    public MercadoPagoClient(@Value("${mercadopago.access-token:}") String accessToken,
                             @Value("${mercadopago.public-key:}") String publicKey) {
        this.accessToken = accessToken;
        this.publicKey = publicKey;
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

    /** Crea un pago con el token de tarjeta generado por el Payment Brick (Checkout Bricks). */
    public Map<String, Object> createPayment(Map<String, Object> body, String idempotencyKey) {
        ensureConfigured();
        try {
            return restClient.post()
                    .uri("/v1/payments")
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JSON_MAP);
        } catch (RestClientResponseException e) {
            log.error("Mercado Pago rechazo el pago: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("Mercado Pago no pudo procesar el pago. Revise los datos de la tarjeta e intente nuevamente.");
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

    /** Public Key para el Payment Brick del frontend; es publica por diseño (solo tokeniza tarjetas). */
    public String getPublicKey() {
        return publicKey;
    }

    private void ensureConfigured() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException("La pasarela de pago no esta configurada (MP_ACCESS_TOKEN).");
        }
    }
}
