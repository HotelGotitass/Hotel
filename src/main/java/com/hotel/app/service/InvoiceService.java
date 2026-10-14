package com.hotel.app.service;

import com.hotel.app.dto.response.InvoiceResponse;
import java.util.List;

public interface InvoiceService {
    InvoiceResponse generateInvoice(Long reservationId);
    InvoiceResponse getInvoiceByReservationId(Long reservationId);
    List<InvoiceResponse> getAllInvoices();
    InvoiceResponse payInvoice(Long id);
    byte[] generatePdf(Long id);
}
