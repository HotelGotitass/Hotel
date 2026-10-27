package com.hotel.app.service;

import com.hotel.app.dto.response.BrickPaymentResponse;
import com.hotel.app.dto.response.PaymentPreferenceResponse;
import com.hotel.app.model.entity.Reservation;
import com.hotel.app.model.enums.PaymentOption;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentService {
    PaymentPreferenceResponse createPreference(Long reservationId, String clientEmail, PaymentOption option);
    BrickPaymentResponse payWithBrick(Long reservationId, String clientEmail, PaymentOption option, Map<String, Object> formData);
    void processPaymentNotification(String paymentId);
    BigDecimal getAmountPaid(Long reservationId);
    BigDecimal getDepositAmount(Reservation reservation);
}
