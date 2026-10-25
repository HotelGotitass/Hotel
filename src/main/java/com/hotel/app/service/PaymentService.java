package com.hotel.app.service;

import com.hotel.app.dto.response.PaymentPreferenceResponse;
import com.hotel.app.model.entity.Reservation;
import com.hotel.app.model.enums.PaymentOption;

import java.math.BigDecimal;

public interface PaymentService {
    PaymentPreferenceResponse createPreference(Long reservationId, String clientEmail, PaymentOption option);
    void processPaymentNotification(String paymentId);
    BigDecimal getAmountPaid(Long reservationId);
    BigDecimal getDepositAmount(Reservation reservation);
}
