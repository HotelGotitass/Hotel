package com.hotel.app.service;

import com.hotel.app.dto.request.ConsumptionRequest;
import com.hotel.app.dto.response.ConsumptionResponse;
import java.util.List;

public interface ConsumptionService {
    ConsumptionResponse addConsumption(ConsumptionRequest request);
    List<ConsumptionResponse> getConsumptionsByReservation(Long reservationId);
    void removeConsumption(Long id);
}
