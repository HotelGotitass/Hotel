package com.hotel.app.service;

import com.hotel.app.billing.SimulatedSunatService;
import com.hotel.app.controller.InvoiceController;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.hotel.app.dto.request.InvoiceRequest;
import com.hotel.app.dto.response.InvoiceResponse;
import com.hotel.app.exception.BusinessException;
import com.hotel.app.model.entity.*;
import com.hotel.app.model.enums.InvoiceDocumentType;
import com.hotel.app.model.enums.InvoiceStatus;
import com.hotel.app.model.enums.ReservationStatus;
import com.hotel.app.repository.*;
import com.hotel.app.service.serviceimpl.InvoiceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Facturacion simulada sin base de datos: repositorios en memoria con Mockito. */
class InvoiceServiceImplTest {

    private final InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    private final InvoiceSeriesRepository seriesRepository = mock(InvoiceSeriesRepository.class);
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final ConsumptionRepository consumptionRepository = mock(ConsumptionRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final Map<String, InvoiceSeries> series = new HashMap<>();
    private InvoiceServiceImpl service;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        SimulatedSunatService sunat = new SimulatedSunatService();
        ReflectionTestUtils.setField(sunat, "issuerRuc", "20600000005");
        service = new InvoiceServiceImpl(invoiceRepository, seriesRepository, reservationRepository,
                consumptionRepository, paymentRepository, sunat, mock(com.hotel.app.service.CashService.class));
        ReflectionTestUtils.setField(service, "issuerRuc", "20600000005");
        ReflectionTestUtils.setField(service, "issuerName", "HOTEL GOTITAS S.A.C.");
        ReflectionTestUtils.setField(service, "issuerAddress", "Av. Ejemplo 123, Lima");

        Guest guest = Guest.builder().id(1L).firstName("Ana").lastName("Quispe")
                .documentType("DNI").documentNumber("45678912").address("Jr. Lima 456").build();
        RoomCategory category = new RoomCategory();
        category.setId(1L);
        category.setName("Suite Panorámica");
        reservation = Reservation.builder().id(10L).guest(guest).roomCategory(category)
                .checkInDate(LocalDate.of(2026, 11, 14)).checkOutDate(LocalDate.of(2026, 11, 17))
                .totalPrice(new BigDecimal("1260.00")).status(ReservationStatus.CHECKED_OUT)
                .bookingCode("GT-7F3A91C2").build();

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(consumptionRepository.findByReservationId(anyLong())).thenReturn(List.of());
        when(paymentRepository.sumAmountByReservationId(anyLong())).thenReturn(new BigDecimal("630.00"));
        when(invoiceRepository.existsByReservationIdAndStatusNot(anyLong(), any())).thenReturn(false);
        when(seriesRepository.findForUpdate(any())).thenAnswer(inv ->
                Optional.of(series.computeIfAbsent(inv.getArgument(0), s -> new InvoiceSeries(s, 0))));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId(100L + series.values().stream().mapToInt(InvoiceSeries::getLastNumber).sum());
            if (i.getIssueDate() == null) i.setIssueDate(LocalDateTime.of(2026, 11, 17, 12, 30));
            when(invoiceRepository.findById(i.getId())).thenReturn(Optional.of(i));
            return i;
        });
    }

    @Test
    void boletaUsesGuestDataAndSequentialNumbers() {
        InvoiceResponse first = service.generateInvoice(10L, null);
        assertEquals("BOLETA", first.getDocumentType());
        assertEquals("B001-00000001", first.getNumber());
        assertEquals("ANA QUISPE", first.getCustomerName());
        assertEquals("45678912", first.getCustomerDocNumber());
        assertEquals(new BigDecimal("1067.80"), first.getSubtotal());
        assertEquals(new BigDecimal("192.20"), first.getTaxAmount());
        assertEquals("ACEPTADO", first.getSunatStatus());
        assertNotNull(first.getSunatHash());

        assertEquals("B001-00000002", service.generateInvoice(10L, null).getNumber());
    }

    @Test
    void facturaRequiresValidRucAndAddress() {
        InvoiceRequest req = new InvoiceRequest();
        req.setDocumentType(InvoiceDocumentType.FACTURA);
        req.setCustomerDocNumber("20600000006");
        req.setCustomerName("Empresa Demo S.A.C.");
        req.setCustomerAddress("Av. Demo 1");
        assertThrows(BusinessException.class, () -> service.generateInvoice(10L, req), "RUC con digito invalido");

        req.setCustomerDocNumber("20600000005");
        req.setCustomerAddress(null);
        assertThrows(BusinessException.class, () -> service.generateInvoice(10L, req), "sin direccion fiscal");

        req.setCustomerAddress("Av. Demo 1");
        InvoiceResponse f = service.generateInvoice(10L, req);
        assertEquals("F001-00000001", f.getNumber());
        assertEquals("RUC", f.getCustomerDocType());
    }

    @Test
    void rejectsSecondActiveInvoiceAndCancelledReservation() {
        when(invoiceRepository.existsByReservationIdAndStatusNot(10L, InvoiceStatus.VOID)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.generateInvoice(10L, null));

        when(invoiceRepository.existsByReservationIdAndStatusNot(10L, InvoiceStatus.VOID)).thenReturn(false);
        reservation.setStatus(ReservationStatus.CANCELLED);
        assertThrows(BusinessException.class, () -> service.generateInvoice(10L, null));
    }

    @Test
    void voidIssuesCreditNoteAndBlocksPayment() throws Exception {
        InvoiceResponse boleta = service.generateInvoice(10L, null);
        InvoiceResponse voided = service.voidInvoice(boleta.getId(), "Cliente solicita factura");
        assertEquals("VOID", voided.getStatus());
        assertEquals("ANULADO", voided.getSunatStatus());
        assertEquals("BC01-00000001", voided.getCreditNoteNumber());
        assertThrows(BusinessException.class, () -> service.voidInvoice(boleta.getId(), "otra vez"));
        assertThrows(BusinessException.class, () -> service.payInvoice(boleta.getId(), com.hotel.app.model.enums.PaymentMethod.CASH, "recepcion@hotel.com"));

        byte[] pdf = service.generatePdf(boleta.getId());
        assertEquals("%PDF", new String(pdf, 0, 4));
        writeSample(pdf, "invoice.pdf.void");
    }

    @Test
    void pdfIsAValidDocumentWithExpectedContent() throws Exception {
        InvoiceResponse boleta = service.generateInvoice(10L, null);
        byte[] pdf = service.generatePdf(boleta.getId());

        // Un lector real debe poder abrirlo (no basta con que empiece por %PDF)
        PdfReader reader = new PdfReader(pdf);
        assertEquals(1, reader.getNumberOfPages());
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();
        assertTrue(text.contains("B001-00000001"), text);
        assertTrue(text.contains("MIL DOSCIENTOS SESENTA CON 00/100 SOLES"), text);
        assertTrue(text.contains("S/ 1260.00"), text);
        writeSample(pdf, "invoice.pdf.out");
    }

    @Test
    void pdfEndpointReturnsIntactPdfBytes() throws Exception {
        InvoiceResponse boleta = service.generateInvoice(10L, null);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new InvoiceController(service)).build();

        MvcResult result = mvc.perform(get("/api/v1/invoices/" + boleta.getId() + "/pdf").accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();
        byte[] body = result.getResponse().getContentAsByteArray();
        assertEquals("%PDF", new String(body, 0, 4));
        assertEquals(1, new PdfReader(body).getNumberOfPages());
    }

    // mvn test -Dinvoice.pdf.out=ruta.pdf deja una muestra para revisarla visualmente
    private static void writeSample(byte[] pdf, String property) throws Exception {
        String out = System.getProperty(property);
        if (out != null) Files.write(Path.of(out), pdf);
    }
}
