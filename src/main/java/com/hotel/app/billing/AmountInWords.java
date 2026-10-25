package com.hotel.app.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Importe en letras para la leyenda de los comprobantes peruanos:
 * 1260.50 -> "MIL DOSCIENTOS SESENTA CON 50/100 SOLES".
 */
public final class AmountInWords {

    private static final String[] UNITS = {
            "", "UN", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
            "VEINTE", "VEINTIUN", "VEINTIDOS", "VEINTITRES", "VEINTICUATRO", "VEINTICINCO", "VEINTISEIS",
            "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"
    };
    private static final String[] TENS = {
            "", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };
    private static final String[] HUNDREDS = {
            "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    private AmountInWords() {}

    public static String soles(BigDecimal amount) {
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        long integer = value.longValue();
        int cents = value.subtract(BigDecimal.valueOf(integer)).movePointRight(2).intValue();
        String words = integer == 0 ? "CERO" : toWords(integer);
        return words + " CON " + String.format("%02d", cents) + "/100 SOLES";
    }

    static String toWords(long n) {
        if (n >= 1_000_000) {
            long millions = n / 1_000_000;
            String head = millions == 1 ? "UN MILLON" : toWords(millions) + " MILLONES";
            long rest = n % 1_000_000;
            return rest == 0 ? head : head + " " + toWords(rest);
        }
        if (n >= 1000) {
            long thousands = n / 1000;
            String head = thousands == 1 ? "MIL" : underThousand((int) thousands) + " MIL";
            long rest = n % 1000;
            return rest == 0 ? head : head + " " + underThousand((int) rest);
        }
        return underThousand((int) n);
    }

    private static String underThousand(int n) {
        if (n == 100) return "CIEN";
        int h = n / 100;
        int rest = n % 100;
        String hundreds = HUNDREDS[h];
        String tail = underHundred(rest);
        if (hundreds.isEmpty()) return tail;
        return tail.isEmpty() ? hundreds : hundreds + " " + tail;
    }

    private static String underHundred(int n) {
        if (n < 30) return UNITS[n];
        int t = n / 10;
        int u = n % 10;
        return u == 0 ? TENS[t] : TENS[t] + " Y " + UNITS[u];
    }
}
