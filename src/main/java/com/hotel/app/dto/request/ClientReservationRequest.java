package com.hotel.app.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ClientReservationRequest {
    @NotNull(message = "La categoria es obligatoria")
    private Long categoryId;

    @NotNull(message = "La fecha de check-in es obligatoria")
    @FutureOrPresent(message = "La fecha de check-in no puede ser en el pasado")
    private LocalDate checkInDate;

    @NotNull(message = "La fecha de check-out es obligatoria")
    @Future(message = "La fecha de check-out debe ser futura")
    private LocalDate checkOutDate;

    @Min(value = 1, message = "Debe haber al menos 1 huesped")
    @Max(value = 20, message = "Numero de huespedes invalido")
    private Integer guests;
}
