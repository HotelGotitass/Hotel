package com.hotel.app.service.serviceimpl;

import com.hotel.app.dto.request.ConsumptionRequest;
import com.hotel.app.dto.response.ConsumptionResponse;
import com.hotel.app.model.entity.Consumption;
import com.hotel.app.model.entity.ExtraService;
import com.hotel.app.model.entity.Reservation;
import com.hotel.app.model.enums.ReservationStatus;
import com.hotel.app.exception.BusinessException;
import com.hotel.app.exception.ResourceNotFoundException;
import com.hotel.app.repository.ConsumptionRepository;
import com.hotel.app.repository.ExtraServiceRepository;
import com.hotel.app.repository.ReservationRepository;
import com.hotel.app.service.ConsumptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsumptionServiceImpl implements ConsumptionService {

    private final ConsumptionRepository consumptionRepository;
    private final ReservationRepository reservationRepository;
    private final ExtraServiceRepository serviceRepository;

    @Override
    @Transactional
    public ConsumptionResponse addConsumption(ConsumptionRequest request) {
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED && reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new BusinessException("Solo se pueden agregar consumos a reservas CONFIRMADAS o con CHECK-IN");
        }

        ExtraService service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        if (!service.getIsActive()) {
            throw new BusinessException("El servicio seleccionado no esta disponible actualmente");
        }

        BigDecimal unitPrice = service.getPrice(); // Congelar precio historico
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        Consumption consumption = Consumption.builder()
                .reservation(reservation)
                .extraService(service)
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .build();

        return mapToResponse(consumptionRepository.save(consumption));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsumptionResponse> getConsumptionsByReservation(Long reservationId) {
        return consumptionRepository.findByReservationId(reservationId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeConsumption(Long id) {
        Consumption consumption = consumptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consumo no encontrado"));
                
        if (consumption.getReservation().getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new BusinessException("No se pueden eliminar consumos de una reserva que ya hizo Check-Out");
        }
        
        consumptionRepository.delete(consumption);
    }

    private ConsumptionResponse mapToResponse(Consumption c) {
        return ConsumptionResponse.builder()
                .id(c.getId())
                .reservationId(c.getReservation().getId())
                .serviceId(c.getExtraService().getId())
                .serviceName(c.getExtraService().getName())
                .quantity(c.getQuantity())
                .unitPrice(c.getUnitPrice())
                .totalPrice(c.getTotalPrice())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
