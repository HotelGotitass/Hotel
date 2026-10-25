package com.hotel.app.service;

import com.hotel.app.dto.request.InvoiceRequest;
import com.hotel.app.dto.response.InvoiceResponse;
import java.util.List;

public interface InvoiceService {
    InvoiceResponse generateInvoice(Long reservationId, InvoiceRequest request);
    InvoiceResponse voidInvoice(Long id, String reason);
    InvoiceResponse getInvoiceByReservationId(Long reservationId);
    List<InvoiceResponse> getAllInvoices();
    InvoiceResponse payInvoice(Long id, com.hotel.app.model.enums.PaymentMethod method, String user);
    byte[] generatePdf(Long id);
    byte[] generatePdfForClient(Long reservationId, String clientEmail);
}
