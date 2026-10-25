package com.hotel.app.service;

import com.hotel.app.dto.request.CashMovementRequest;
import com.hotel.app.dto.request.CloseCashRequest;
import com.hotel.app.dto.response.CashSessionResponse;
import com.hotel.app.dto.response.PaymentResponse;
import com.hotel.app.model.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CashService {
    Optional<CashSessionResponse> getCurrentSession();
    CashSessionResponse openSession(BigDecimal openingAmount, String user);
    CashSessionResponse addMovement(CashMovementRequest request, String user);
    CashSessionResponse closeSession(CloseCashRequest request, String user, boolean isAdmin);
    List<CashSessionResponse> getHistory();
    CashSessionResponse getSession(Long id);

    /** Cobro en recepcion: exige caja abierta y registra el pago y el movimiento de caja. */
    PaymentResponse registerReservationPayment(Long reservationId, BigDecimal amount, PaymentMethod method, String reference, String user);
    List<PaymentResponse> getReservationPayments(Long reservationId);

    /** Devolucion en efectivo: exige caja abierta con efectivo suficiente y la registra como egreso. */
    void registerCashRefund(Long reservationId, BigDecimal amount, String user);
}
