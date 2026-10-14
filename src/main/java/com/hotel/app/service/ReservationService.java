package com.hotel.app.service;

import com.hotel.app.dto.request.ReservationRequest;
import com.hotel.app.dto.response.CategoryAvailabilityResponse;
import com.hotel.app.dto.response.ReservationResponse;
import com.hotel.app.model.enums.RefundReason;

import java.time.LocalDate;
import java.util.List;

public interface ReservationService {
    List<CategoryAvailabilityResponse> searchAvailability(LocalDate checkIn, LocalDate checkOut);
    ReservationResponse createReservation(ReservationRequest request);
    List<ReservationResponse> getAllReservations();
    ReservationResponse assignRoomAndCheckIn(Long reservationId, Long roomId);
    ReservationResponse cancelReservationByClient(Long reservationId, String clientEmail);
    ReservationResponse cancelReservationByAdmin(Long reservationId, RefundReason refundReason);
    ReservationResponse checkOut(Long reservationId);
    ReservationResponse createWebReservation(com.hotel.app.dto.request.ClientReservationRequest request, String clientEmail);
    List<ReservationResponse> getClientReservations(String clientEmail);

    ReservationResponse getReservationByCode(String bookingCode);

    ReservationResponse modifyReservationDates(Long reservationId, String clientEmail, LocalDate newCheckIn, LocalDate newCheckOut);
}
