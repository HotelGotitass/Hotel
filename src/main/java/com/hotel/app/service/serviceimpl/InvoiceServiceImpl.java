package com.hotel.app.service.serviceimpl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.hotel.app.dto.response.InvoiceResponse;
import com.hotel.app.model.entity.Consumption;
import com.hotel.app.model.entity.Invoice;
import com.hotel.app.model.enums.InvoiceStatus;
import com.hotel.app.model.entity.Reservation;
import com.hotel.app.exception.BusinessException;
import com.hotel.app.exception.ResourceNotFoundException;
import com.hotel.app.repository.ConsumptionRepository;
import com.hotel.app.repository.InvoiceRepository;
import com.hotel.app.repository.ReservationRepository;
import com.hotel.app.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ReservationRepository reservationRepository;
    private final ConsumptionRepository consumptionRepository;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.18");

    @Override
    @Transactional
    public InvoiceResponse generateInvoice(Long reservationId) {
        if (invoiceRepository.existsByReservationId(reservationId)) {
            throw new BusinessException("Ya existe una factura para esta reserva");
        }
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));
        List<Consumption> consumptions = consumptionRepository.findByReservationId(reservationId);
        
        BigDecimal roomTotal = reservation.getTotalPrice();
        BigDecimal consumptionsTotal = consumptions.stream()
                .map(Consumption::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = roomTotal.add(consumptionsTotal);
        BigDecimal subtotal = total.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
        BigDecimal tax = total.subtract(subtotal);

        Invoice invoice = Invoice.builder()
                .reservation(reservation)
                .subtotal(subtotal)
                .taxAmount(tax)
                .totalAmount(total)
                .status(InvoiceStatus.PENDING)
                .build();
        return mapToResponse(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByReservationId(Long reservationId) {
        Invoice invoice = invoiceRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No hay factura para esta reserva"));
        return mapToResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InvoiceResponse payInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada"));
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("La factura ya se encuentra pagada");
        }
        invoice.setStatus(InvoiceStatus.PAID);
        return mapToResponse(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePdf(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            Paragraph title = new Paragraph("GOTITAS HOTEL - FACTURA #" + invoice.getId(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Reservation res = invoice.getReservation();
            document.add(new Paragraph("Cliente: " + res.getGuest().getFirstName() + " " + res.getGuest().getLastName(), normalFont));
            document.add(new Paragraph("Documento: " + res.getGuest().getDocumentNumber(), normalFont));
            document.add(new Paragraph("Fecha de Emision: " + invoice.getIssueDate().toLocalDate(), normalFont));
            document.add(new Paragraph("Estado: " + invoice.getStatus().name(), normalFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell(new PdfPCell(new Phrase("Concepto", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Cant.", headerFont)));
            table.addCell(new PdfPCell(new Phrase("P.Unitario", headerFont)));
            table.addCell(new PdfPCell(new Phrase("Subtotal", headerFont)));

            // Agregar Reserva
            table.addCell(new Phrase("Estadia - " + res.getRoomCategory().getName(), normalFont));
            table.addCell(new Phrase("1", normalFont));
            table.addCell(new Phrase("$" + res.getTotalPrice(), normalFont));
            table.addCell(new Phrase("$" + res.getTotalPrice(), normalFont));

            // Agregar Consumos
            List<Consumption> consumptions = consumptionRepository.findByReservationId(res.getId());
            for (Consumption c : consumptions) {
                table.addCell(new Phrase(c.getExtraService().getName(), normalFont));
                table.addCell(new Phrase(String.valueOf(c.getQuantity()), normalFont));
                table.addCell(new Phrase("$" + c.getUnitPrice(), normalFont));
                table.addCell(new Phrase("$" + c.getTotalPrice(), normalFont));
            }

            document.add(table);
            document.add(new Paragraph(" "));

            Paragraph subtotals = new Paragraph();
            subtotals.setAlignment(Element.ALIGN_RIGHT);
            subtotals.add(new Phrase("Subtotal: $" + invoice.getSubtotal() + "\n", normalFont));
            subtotals.add(new Phrase("Impuestos (18%): $" + invoice.getTaxAmount() + "\n", normalFont));
            subtotals.add(new Phrase("TOTAL A PAGAR: $" + invoice.getTotalAmount(), titleFont));
            document.add(subtotals);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("Error al generar el PDF de la factura: " + e.getMessage());
        }
    }

    private InvoiceResponse mapToResponse(Invoice inv) {
        return InvoiceResponse.builder()
                .id(inv.getId())
                .reservationId(inv.getReservation().getId())
                .guestName(inv.getReservation().getGuest().getFirstName() + " " + inv.getReservation().getGuest().getLastName())
                .documentNumber(inv.getReservation().getGuest().getDocumentNumber())
                .subtotal(inv.getSubtotal())
                .taxAmount(inv.getTaxAmount())
                .totalAmount(inv.getTotalAmount())
                .status(inv.getStatus().name())
                .issueDate(inv.getIssueDate())
                .build();
    }
}

