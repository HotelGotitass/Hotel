package com.hotel.app.model.enums;

/** Tipos de comprobante (catalogo 01 de SUNAT) con sus series simuladas. */
public enum InvoiceDocumentType {
    FACTURA("01", "F001", "FC01", "FACTURA ELECTRÓNICA"),
    BOLETA("03", "B001", "BC01", "BOLETA DE VENTA ELECTRÓNICA");

    private final String sunatCode;
    private final String series;
    private final String creditNoteSeries;
    private final String label;

    InvoiceDocumentType(String sunatCode, String series, String creditNoteSeries, String label) {
        this.sunatCode = sunatCode;
        this.series = series;
        this.creditNoteSeries = creditNoteSeries;
        this.label = label;
    }

    public String getSunatCode() { return sunatCode; }
    public String getSeries() { return series; }
    public String getCreditNoteSeries() { return creditNoteSeries; }
    public String getLabel() { return label; }
}
