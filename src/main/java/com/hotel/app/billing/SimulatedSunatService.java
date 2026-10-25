package com.hotel.app.billing;

import com.hotel.app.model.entity.Invoice;
import com.hotel.app.model.enums.SunatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Sustituto de SUNAT/OSE: NO se conecta a ninguna entidad. Calcula un hash del comprobante,
 * lo marca como ACEPTADO con el mensaje que devolveria SUNAT y arma el texto del QR
 * con el formato oficial, para que el flujo y el PDF se vean como en produccion.
 */
@Component
public class SimulatedSunatService {

    private static final DateTimeFormatter QR_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${billing.issuer.ruc}")
    private String issuerRuc;

    public void accept(Invoice invoice) {
        invoice.setSunatHash(hash(invoice));
        invoice.setSunatStatus(SunatStatus.ACEPTADO);
        String kind = invoice.getDocumentType().name().charAt(0) + invoice.getDocumentType().name().substring(1).toLowerCase();
        invoice.setSunatMessage("La " + kind + " numero " + invoice.getNumber() + ", ha sido aceptada (SIMULADO)");
    }

    public void voidWithCreditNote(Invoice invoice) {
        invoice.setSunatStatus(SunatStatus.ANULADO);
        invoice.setSunatMessage("Anulada con Nota de Credito " + invoice.getCreditNoteNumber() + " (SIMULADO)");
    }

    /** RUC|TIPO|SERIE|NUMERO|IGV|TOTAL|FECHA|TIPO_DOC_ADQ|NUM_DOC_ADQ|HASH| (formato del QR de SUNAT) */
    public String qrContent(Invoice invoice) {
        return String.join("|",
                issuerRuc,
                invoice.getDocumentType().getSunatCode(),
                invoice.getSeries(),
                String.valueOf(invoice.getCorrelative()),
                invoice.getTaxAmount().toPlainString(),
                invoice.getTotalAmount().toPlainString(),
                invoice.getIssueDate() != null ? invoice.getIssueDate().format(QR_DATE) : "",
                PeruDocuments.sunatCode(invoice.getCustomerDocType()),
                invoice.getCustomerDocNumber() != null ? invoice.getCustomerDocNumber() : "",
                invoice.getSunatHash() != null ? invoice.getSunatHash() : "") + "|";
    }

    private String hash(Invoice invoice) {
        String canonical = String.join("|", issuerRuc, invoice.getDocumentType().getSunatCode(), invoice.getNumber(),
                invoice.getSubtotal().toPlainString(), invoice.getTaxAmount().toPlainString(),
                invoice.getTotalAmount().toPlainString(), String.valueOf(invoice.getCustomerDocNumber()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
