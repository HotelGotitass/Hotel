package com.hotel.app.service.serviceimpl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.hotel.app.billing.AmountInWords;
import com.hotel.app.billing.PeruDocuments;
import com.hotel.app.billing.SimulatedSunatService;
import com.hotel.app.dto.request.InvoiceRequest;
import com.hotel.app.dto.response.InvoiceResponse;
import com.hotel.app.exception.BusinessException;
import com.hotel.app.exception.ResourceNotFoundException;
import com.hotel.app.model.entity.Consumption;
import com.hotel.app.model.entity.Guest;
import com.hotel.app.model.entity.Invoice;
import com.hotel.app.model.entity.InvoiceSeries;
import com.hotel.app.model.entity.Reservation;
import com.hotel.app.model.enums.InvoiceDocumentType;
import com.hotel.app.model.enums.InvoiceStatus;
import com.hotel.app.model.enums.ReservationStatus;
import com.hotel.app.repository.ConsumptionRepository;
import com.hotel.app.repository.InvoiceRepository;
import com.hotel.app.repository.InvoiceSeriesRepository;
import com.hotel.app.repository.PaymentRepository;
import com.hotel.app.repository.ReservationRepository;
import com.hotel.app.service.InvoiceService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private static final BigDecimal IGV_FACTOR = new BigDecimal("1.18");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color INK = new Color(14, 12, 11);
    private static final Color MUTED = new Color(110, 104, 98);
    private static final Color LINE = new Color(217, 205, 187);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceSeriesRepository seriesRepository;
    private final ReservationRepository reservationRepository;
    private final ConsumptionRepository consumptionRepository;
    private final PaymentRepository paymentRepository;
    private final SimulatedSunatService sunat;
    private final com.hotel.app.service.CashService cashService;

    @Value("${billing.issuer.ruc}")
    private String issuerRuc;

    @Value("${billing.issuer.name}")
    private String issuerName;

    @Value("${billing.issuer.address}")
    private String issuerAddress;

    @Override
    @Transactional
    public InvoiceResponse generateInvoice(Long reservationId, InvoiceRequest request) {
        if (invoiceRepository.existsByReservationIdAndStatusNot(reservationId, InvoiceStatus.VOID)) {
            throw new BusinessException("La reserva ya tiene un comprobante vigente. Anúlelo para emitir uno nuevo.");
        }
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("No se puede emitir comprobante de una reserva cancelada");
        }

        InvoiceRequest req = request != null ? request : new InvoiceRequest();
        InvoiceDocumentType type = req.getDocumentType() != null ? req.getDocumentType() : InvoiceDocumentType.BOLETA;
        Guest guest = reservation.getGuest();
        boolean factura = type == InvoiceDocumentType.FACTURA;

        // Boleta: por defecto los datos del huesped. Factura: datos de la empresa, siempre explicitos.
        String docType = PeruDocuments.normalizeType(firstNonBlank(req.getCustomerDocType(), factura ? "RUC" : guest.getDocumentType()));
        String docNumber = trim(firstNonBlank(req.getCustomerDocNumber(), factura ? null : guest.getDocumentNumber()));
        String name = trim(firstNonBlank(req.getCustomerName(), factura ? null : guest.getFirstName() + " " + guest.getLastName()));
        String address = trim(firstNonBlank(req.getCustomerAddress(), factura ? null : guest.getAddress()));
        validateCustomer(type, docType, docNumber, name, address);

        BigDecimal total = reservation.getTotalPrice().add(consumptionsTotal(reservationId)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = total.divide(IGV_FACTOR, 2, RoundingMode.HALF_UP);

        Invoice invoice = Invoice.builder()
                .reservation(reservation)
                .documentType(type)
                .series(type.getSeries())
                .correlative(nextNumber(type.getSeries()))
                .customerDocType(docType)
                .customerDocNumber(docNumber)
                .customerName(name.toUpperCase())
                .customerAddress(address)
                .subtotal(subtotal)
                .taxAmount(total.subtract(subtotal))
                .totalAmount(total)
                .status(InvoiceStatus.PENDING)
                .build();
        sunat.accept(invoice);
        return mapToResponse(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional
    public InvoiceResponse voidInvoice(Long id, String reason) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante no encontrado"));
        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new BusinessException("El comprobante ya se encuentra anulado");
        }
        String creditSeries = invoice.getDocumentType().getCreditNoteSeries();
        invoice.setCreditNoteSeries(creditSeries);
        invoice.setCreditNoteCorrelative(nextNumber(creditSeries));
        invoice.setStatus(InvoiceStatus.VOID);
        invoice.setVoidReason(reason.trim());
        invoice.setVoidedAt(LocalDateTime.now());
        sunat.voidWithCreditNote(invoice);
        return mapToResponse(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByReservationId(Long reservationId) {
        return mapToResponse(activeInvoice(reservationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAllByOrderByIdDesc().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InvoiceResponse payInvoice(Long id, com.hotel.app.model.enums.PaymentMethod method, String user) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante no encontrado"));
        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new BusinessException("No se puede cobrar un comprobante anulado");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("El comprobante ya se encuentra pagado");
        }
        Long reservationId = invoice.getReservation().getId();
        BigDecimal balance = invoice.getTotalAmount().subtract(paymentRepository.sumAmountByReservationId(reservationId));
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            // Registra el cobro del saldo en la caja abierta; al completar el total marca el comprobante como PAGADO
            cashService.registerReservationPayment(reservationId, balance, method, "Cobro " + invoice.getNumber(), user);
            invoice = invoiceRepository.findById(id).orElseThrow();
        }
        invoice.setStatus(InvoiceStatus.PAID);
        return mapToResponse(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePdfForClient(Long reservationId, String clientEmail) {
        Invoice invoice = invoiceRepository.findFirstByReservationIdAndStatusNotOrderByIdDesc(reservationId, InvoiceStatus.VOID)
                .orElseThrow(() -> new ResourceNotFoundException("La factura aun no ha sido emitida por el hotel"));
        Reservation res = invoice.getReservation();
        if (res.getUser() == null || !res.getUser().getEmail().equals(clientEmail)) {
            throw new ResourceNotFoundException("La factura aun no ha sido emitida por el hotel");
        }
        return generatePdf(invoice.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePdf(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante no encontrado"));
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            writeWatermarks(writer, invoice);
            writeHeader(document, invoice);
            writeCustomer(document, invoice);
            writeItems(document, invoice);
            writeTotals(document, invoice);
            writeFooter(document, writer, invoice);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Error al generar el PDF del comprobante: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- emision

    private void validateCustomer(InvoiceDocumentType type, String docType, String docNumber, String name, String address) {
        if (docNumber == null) throw new BusinessException("El número de documento del cliente es obligatorio");
        if (name == null) throw new BusinessException("El nombre o razón social del cliente es obligatorio");

        if (type == InvoiceDocumentType.FACTURA) {
            if (!"RUC".equals(docType)) throw new BusinessException("La factura solo puede emitirse a un RUC");
            if (!PeruDocuments.isValidRuc(docNumber)) throw new BusinessException("RUC inválido: verifique los 11 dígitos");
            if (address == null) throw new BusinessException("La factura requiere la dirección fiscal del cliente");
            return;
        }
        switch (docType) {
            case "DNI" -> { if (!PeruDocuments.isValidDni(docNumber)) throw new BusinessException("DNI inválido: debe tener 8 dígitos"); }
            case "RUC" -> { if (!PeruDocuments.isValidRuc(docNumber)) throw new BusinessException("RUC inválido: verifique los 11 dígitos"); }
            default -> { if (!docNumber.matches("[A-Za-z0-9]{4,15}")) throw new BusinessException("Número de documento inválido"); }
        }
    }

    private int nextNumber(String series) {
        InvoiceSeries s = seriesRepository.findForUpdate(series)
                .orElseGet(() -> new InvoiceSeries(series, 0));
        s.setLastNumber(s.getLastNumber() + 1);
        seriesRepository.save(s);
        return s.getLastNumber();
    }

    private Invoice activeInvoice(Long reservationId) {
        return invoiceRepository.findFirstByReservationIdAndStatusNotOrderByIdDesc(reservationId, InvoiceStatus.VOID)
                .orElseThrow(() -> new ResourceNotFoundException("No hay comprobante vigente para esta reserva"));
    }

    private BigDecimal consumptionsTotal(Long reservationId) {
        return consumptionRepository.findByReservationId(reservationId).stream()
                .map(Consumption::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // ---------------------------------------------------------------- PDF

    private static Font font(int style, float size, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.WINANSI, size, style, color);
    }

    private static PdfPCell cell(Phrase phrase, int align, boolean border) {
        PdfPCell c = new PdfPCell(phrase);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5);
        c.setBorder(border ? Rectangle.BOTTOM : Rectangle.NO_BORDER);
        c.setBorderColor(LINE);
        return c;
    }

    private void writeWatermarks(PdfWriter writer, Invoice invoice) throws Exception {
        PdfContentByte under = writer.getDirectContentUnder();
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
        PdfGState gs = new PdfGState();
        gs.setFillOpacity(0.07f);
        under.saveState();
        under.setGState(gs);
        under.beginText();
        under.setFontAndSize(bf, 90);
        under.setColorFill(INK);
        under.showTextAligned(Element.ALIGN_CENTER, "SIMULADO", 297, 420, 45);
        under.endText();
        under.restoreState();

        if (invoice.getStatus() == InvoiceStatus.VOID) {
            PdfContentByte over = writer.getDirectContent();
            PdfGState red = new PdfGState();
            red.setFillOpacity(0.35f);
            over.saveState();
            over.setGState(red);
            over.beginText();
            over.setFontAndSize(bf, 72);
            over.setColorFill(new Color(180, 30, 30));
            over.showTextAligned(Element.ALIGN_CENTER, "ANULADO", 297, 520, 30);
            over.endText();
            over.restoreState();
        }
    }

    private void writeHeader(Document document, Invoice invoice) throws DocumentException {
        PdfPTable header = new PdfPTable(new float[]{62, 38});
        header.setWidthPercentage(100);

        Paragraph issuer = new Paragraph();
        issuer.add(new Chunk(issuerName + "\n", font(Font.BOLD, 15, INK)));
        issuer.add(new Chunk("Servicios de hospedaje\n", font(Font.NORMAL, 9, MUTED)));
        issuer.add(new Chunk(issuerAddress + "\n", font(Font.NORMAL, 9, MUTED)));
        PdfPCell left = new PdfPCell(issuer);
        left.setBorder(Rectangle.NO_BORDER);
        left.setPaddingTop(6);
        header.addCell(left);

        Paragraph box = new Paragraph();
        box.setAlignment(Element.ALIGN_CENTER);
        box.add(new Chunk("R.U.C. N° " + issuerRuc + "\n", font(Font.BOLD, 11, INK)));
        box.add(new Chunk(invoice.getDocumentType().getLabel() + "\n", font(Font.BOLD, 10, INK)));
        box.add(new Chunk(invoice.getNumber(), font(Font.BOLD, 13, INK)));
        PdfPCell right = new PdfPCell(box);
        right.setHorizontalAlignment(Element.ALIGN_CENTER);
        right.setBorderWidth(1.2f);
        right.setBorderColor(INK);
        right.setPadding(10);
        header.addCell(right);

        document.add(header);
        document.add(new Paragraph(" "));
    }

    private void writeCustomer(Document document, Invoice invoice) throws DocumentException {
        Reservation res = invoice.getReservation();
        PdfPTable t = new PdfPTable(new float[]{22, 78});
        t.setWidthPercentage(100);
        String[][] rows = {
                {"Fecha de emisión", invoice.getIssueDate() != null ? invoice.getIssueDate().format(DATE_TIME) : "-"},
                {invoice.getDocumentType() == InvoiceDocumentType.FACTURA ? "Razón social" : "Cliente", invoice.getCustomerName()},
                {invoice.getCustomerDocType(), invoice.getCustomerDocNumber()},
                {"Dirección", invoice.getCustomerAddress() != null ? invoice.getCustomerAddress() : "-"},
                {"Reserva", res.getBookingCode() + "  ·  " + res.getCheckInDate() + " al " + res.getCheckOutDate()},
                {"Moneda", "SOLES (PEN)"},
        };
        for (String[] r : rows) {
            t.addCell(cell(new Phrase(r[0], font(Font.BOLD, 9, MUTED)), Element.ALIGN_LEFT, false));
            t.addCell(cell(new Phrase(r[1], font(Font.NORMAL, 9, INK)), Element.ALIGN_LEFT, false));
        }
        document.add(t);
        document.add(new Paragraph(" "));
    }

    private void writeItems(Document document, Invoice invoice) throws DocumentException {
        Reservation res = invoice.getReservation();
        PdfPTable t = new PdfPTable(new float[]{9, 9, 52, 15, 15});
        t.setWidthPercentage(100);
        for (String h : new String[]{"CANT.", "UND.", "DESCRIPCIÓN", "P. UNIT.", "IMPORTE"}) {
            PdfPCell c = cell(new Phrase(h, font(Font.BOLD, 8, Color.WHITE)), Element.ALIGN_CENTER, false);
            c.setBackgroundColor(INK);
            t.addCell(c);
        }

        long nights = Math.max(1, ChronoUnit.DAYS.between(res.getCheckInDate(), res.getCheckOutDate()));
        BigDecimal roomUnit = res.getTotalPrice().divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP);
        addItem(t, String.valueOf(nights), "NIU", "Hospedaje - " + res.getRoomCategory().getName()
                + " (" + nights + " noche" + (nights > 1 ? "s" : "") + ")", roomUnit, res.getTotalPrice());

        for (Consumption c : consumptionRepository.findByReservationId(res.getId())) {
            addItem(t, String.valueOf(c.getQuantity()), "ZZ", c.getExtraService().getName(), c.getUnitPrice(), c.getTotalPrice());
        }
        document.add(t);
    }

    private void addItem(PdfPTable t, String qty, String unit, String description, BigDecimal unitPrice, BigDecimal amount) {
        Font f = font(Font.NORMAL, 9, INK);
        t.addCell(cell(new Phrase(qty, f), Element.ALIGN_CENTER, true));
        t.addCell(cell(new Phrase(unit, f), Element.ALIGN_CENTER, true));
        t.addCell(cell(new Phrase(description, f), Element.ALIGN_LEFT, true));
        t.addCell(cell(new Phrase(money(unitPrice), f), Element.ALIGN_RIGHT, true));
        t.addCell(cell(new Phrase(money(amount), f), Element.ALIGN_RIGHT, true));
    }

    private void writeTotals(Document document, Invoice invoice) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{58, 27, 15});
        t.setWidthPercentage(100);
        t.setSpacingBefore(8);
        String[][] rows = {
                {"OP. GRAVADA", money(invoice.getSubtotal())},
                {"IGV (18%)", money(invoice.getTaxAmount())},
                {"IMPORTE TOTAL", money(invoice.getTotalAmount())},
        };
        for (int i = 0; i < rows.length; i++) {
            boolean last = i == rows.length - 1;
            t.addCell(cell(new Phrase(""), Element.ALIGN_LEFT, false));
            t.addCell(cell(new Phrase(rows[i][0], font(Font.BOLD, last ? 10 : 9, last ? INK : MUTED)), Element.ALIGN_RIGHT, false));
            t.addCell(cell(new Phrase(rows[i][1], font(Font.BOLD, last ? 11 : 9, INK)), Element.ALIGN_RIGHT, false));
        }
        document.add(t);

        Paragraph words = new Paragraph("SON: " + AmountInWords.soles(invoice.getTotalAmount()), font(Font.BOLD, 9, INK));
        words.setSpacingBefore(6);
        document.add(words);

        BigDecimal paid = paymentRepository.sumAmountByReservationId(invoice.getReservation().getId());
        BigDecimal balance = invoice.getTotalAmount().subtract(paid).max(BigDecimal.ZERO);
        Paragraph payments = new Paragraph("Pagos recibidos: " + money(paid) + "   ·   Saldo: " + money(balance)
                + "   ·   Estado: " + (invoice.getStatus() == InvoiceStatus.PAID ? "PAGADO" : invoice.getStatus() == InvoiceStatus.VOID ? "ANULADO" : "PENDIENTE"),
                font(Font.NORMAL, 8, MUTED));
        payments.setSpacingBefore(4);
        document.add(payments);
    }

    private void writeFooter(Document document, PdfWriter writer, Invoice invoice) throws Exception {
        document.add(new Paragraph(" "));
        PdfPTable t = new PdfPTable(new float[]{20, 80});
        t.setWidthPercentage(100);

        PdfPCell qrCell = new PdfPCell(qrVector(writer, sunat.qrContent(invoice), 92), false);
        qrCell.setBorder(Rectangle.NO_BORDER);
        t.addCell(qrCell);

        Paragraph info = new Paragraph();
        info.add(new Chunk("Representación impresa de la " + invoice.getDocumentType().getLabel().toLowerCase() + ".\n", font(Font.NORMAL, 8, INK)));
        info.add(new Chunk("Resumen (hash): " + invoice.getSunatHash() + "\n", font(Font.NORMAL, 7, MUTED)));
        info.add(new Chunk("Estado: " + invoice.getSunatMessage() + "\n", font(Font.NORMAL, 7, MUTED)));
        if (invoice.getStatus() == InvoiceStatus.VOID) {
            info.add(new Chunk("Anulado mediante Nota de Crédito " + invoice.getCreditNoteNumber()
                    + " · Motivo: " + invoice.getVoidReason() + "\n", font(Font.BOLD, 8, new Color(180, 30, 30))));
        }
        info.add(new Chunk("\nDOCUMENTO SIMULADO: no fue enviado a SUNAT ni a ningún OSE/PSE y no tiene validez tributaria.",
                font(Font.BOLD, 7, MUTED)));
        PdfPCell infoCell = new PdfPCell(info);
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        infoCell.setPaddingLeft(8);
        t.addCell(infoCell);
        document.add(t);
    }

    // QR vectorial (un rectangulo por modulo): no usa java.awt.image, que puede fallar en imagenes
    // Docker minimas (Alpine), y se imprime nitido a cualquier tamaño
    private static Image qrVector(PdfWriter writer, String content, float size) throws Exception {
        BitMatrix m = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, Map.of(EncodeHintType.MARGIN, 1));
        int modules = m.getWidth();
        float cellSize = size / modules;
        PdfTemplate tpl = writer.getDirectContent().createTemplate(size, size);
        tpl.setColorFill(Color.BLACK);
        for (int x = 0; x < modules; x++) {
            for (int y = 0; y < modules; y++) {
                if (m.get(x, y)) tpl.rectangle(x * cellSize, size - (y + 1) * cellSize, cellSize, cellSize);
            }
        }
        tpl.fill();
        return Image.getInstance(tpl);
    }

    private static String money(BigDecimal value) {
        return "S/ " + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    // ---------------------------------------------------------------- mapping

    private InvoiceResponse mapToResponse(Invoice inv) {
        Reservation res = inv.getReservation();
        return InvoiceResponse.builder()
                .id(inv.getId())
                .reservationId(res.getId())
                .bookingCode(res.getBookingCode())
                .guestName(res.getGuest().getFirstName() + " " + res.getGuest().getLastName())
                .documentNumber(res.getGuest().getDocumentNumber())
                .subtotal(inv.getSubtotal())
                .taxAmount(inv.getTaxAmount())
                .totalAmount(inv.getTotalAmount())
                .status(inv.getStatus().name())
                .issueDate(inv.getIssueDate())
                .documentType(inv.getDocumentType().name())
                .number(inv.getNumber())
                .customerDocType(inv.getCustomerDocType())
                .customerDocNumber(inv.getCustomerDocNumber())
                .customerName(inv.getCustomerName())
                .customerAddress(inv.getCustomerAddress())
                .sunatStatus(inv.getSunatStatus() != null ? inv.getSunatStatus().name() : null)
                .sunatMessage(inv.getSunatMessage())
                .sunatHash(inv.getSunatHash())
                .voidReason(inv.getVoidReason())
                .voidedAt(inv.getVoidedAt())
                .creditNoteNumber(inv.getCreditNoteNumber())
                .build();
    }
}
