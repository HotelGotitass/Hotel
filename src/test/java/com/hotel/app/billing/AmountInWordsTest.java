package com.hotel.app.billing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmountInWordsTest {

    private static String w(String amount) {
        return AmountInWords.soles(new BigDecimal(amount));
    }

    @Test
    void basicAmounts() {
        assertEquals("CERO CON 00/100 SOLES", w("0"));
        assertEquals("UN CON 00/100 SOLES", w("1"));
        assertEquals("QUINCE CON 50/100 SOLES", w("15.50"));
        assertEquals("VEINTIUN CON 00/100 SOLES", w("21"));
        assertEquals("TREINTA Y CINCO CON 99/100 SOLES", w("35.99"));
    }

    @Test
    void hundredsAndThousands() {
        assertEquals("CIEN CON 00/100 SOLES", w("100"));
        assertEquals("CIENTO UN CON 00/100 SOLES", w("101"));
        assertEquals("QUINIENTOS CON 00/100 SOLES", w("500"));
        assertEquals("MIL CON 00/100 SOLES", w("1000"));
        assertEquals("MIL DOSCIENTOS SESENTA CON 00/100 SOLES", w("1260"));
        assertEquals("DOS MIL CUATROCIENTOS SETENTA Y OCHO CON 82/100 SOLES", w("2478.82"));
        assertEquals("CIEN MIL CON 00/100 SOLES", w("100000"));
    }

    @Test
    void millionsAndRounding() {
        assertEquals("UN MILLON CON 00/100 SOLES", w("1000000"));
        assertEquals("DOS MILLONES TRESCIENTOS MIL CON 00/100 SOLES", w("2300000"));
        assertEquals("DIEZ CON 01/100 SOLES", w("10.005"));
    }
}
