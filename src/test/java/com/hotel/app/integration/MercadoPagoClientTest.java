package com.hotel.app.integration;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class MercadoPagoClientTest {

    private static final String SECRET = "test-webhook-secret";
    private final MercadoPagoClient client = new MercadoPagoClient("TEST-token", SECRET);

    private static String sign(String manifest) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void acceptsValidSignature() throws Exception {
        String v1 = sign("id:123456;request-id:req-1;ts:1700000000;");
        assertTrue(client.isValidSignature("ts=1700000000,v1=" + v1, "req-1", "123456"));
    }

    @Test
    void acceptsSignatureWithoutRequestId() throws Exception {
        String v1 = sign("id:123456;ts:1700000000;");
        assertTrue(client.isValidSignature("ts=1700000000,v1=" + v1, null, "123456"));
    }

    @Test
    void rejectsTamperedPaymentId() throws Exception {
        String v1 = sign("id:123456;request-id:req-1;ts:1700000000;");
        assertFalse(client.isValidSignature("ts=1700000000,v1=" + v1, "req-1", "999999"));
    }

    @Test
    void rejectsMissingOrMalformedHeader() {
        assertFalse(client.isValidSignature(null, "req-1", "123456"));
        assertFalse(client.isValidSignature("mock-signature-para-saltar-seguridad", "req-1", "123456"));
        assertFalse(client.isValidSignature("ts=1700000000", "req-1", "123456"));
    }
}
