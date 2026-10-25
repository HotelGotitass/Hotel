package com.hotel.app.billing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PeruDocumentsTest {

    @Test
    void rucCheckDigit() {
        assertTrue(PeruDocuments.isValidRuc("20600000005"), "RUC por defecto del emisor simulado");
        assertFalse(PeruDocuments.isValidRuc("20600000006"), "digito verificador incorrecto");
        assertFalse(PeruDocuments.isValidRuc("30600000005"), "prefijo invalido");
        assertFalse(PeruDocuments.isValidRuc("2060000000"), "10 digitos");
        assertFalse(PeruDocuments.isValidRuc(null));
    }

    @Test
    void dni() {
        assertTrue(PeruDocuments.isValidDni("45678912"));
        assertFalse(PeruDocuments.isValidDni("4567891"));
        assertFalse(PeruDocuments.isValidDni("4567891A"));
    }

    @Test
    void sunatCodes() {
        assertEquals("1", PeruDocuments.sunatCode("DNI"));
        assertEquals("6", PeruDocuments.sunatCode("ruc"));
        assertEquals("4", PeruDocuments.sunatCode("Carnet de Extranjeria"));
        assertEquals("7", PeruDocuments.sunatCode("PASAPORTE"));
        assertEquals("0", PeruDocuments.sunatCode("OTRO"));
    }
}
