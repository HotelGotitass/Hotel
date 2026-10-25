package com.hotel.app.billing;

import java.util.Locale;

/** Validacion y codigos de documentos de identidad peruanos (catalogo 06 de SUNAT). */
public final class PeruDocuments {

    private static final int[] RUC_WEIGHTS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private PeruDocuments() {}

    /** RUC: 11 digitos, prefijo valido y digito verificador modulo 11. */
    public static boolean isValidRuc(String ruc) {
        if (ruc == null || !ruc.matches("(10|15|16|17|20)\\d{9}")) return false;
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += (ruc.charAt(i) - '0') * RUC_WEIGHTS[i];
        }
        int check = 11 - (sum % 11);
        if (check == 10) check = 0;
        if (check == 11) check = 1;
        return check == ruc.charAt(10) - '0';
    }

    public static boolean isValidDni(String dni) {
        return dni != null && dni.matches("\\d{8}");
    }

    public static String normalizeType(String type) {
        if (type == null) return "DNI";
        String t = type.trim().toUpperCase(Locale.ROOT).replace(" ", "_");
        return switch (t) {
            case "CE", "CARNET_EXTRANJERIA", "CARNET_DE_EXTRANJERIA" -> "CE";
            case "PASAPORTE", "PASSPORT" -> "PASAPORTE";
            default -> t;
        };
    }

    /** Codigo SUNAT del tipo de documento (usado en el QR). */
    public static String sunatCode(String type) {
        return switch (normalizeType(type)) {
            case "DNI" -> "1";
            case "CE" -> "4";
            case "RUC" -> "6";
            case "PASAPORTE" -> "7";
            default -> "0";
        };
    }
}
