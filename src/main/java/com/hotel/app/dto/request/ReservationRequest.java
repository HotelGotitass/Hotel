package com.hotel.app.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ReservationRequest {
    @NotNull(message = "Guest ID is required")
    private Long guestId;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in must be today or in the future")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date is required")
    @Future(message = "Check-out must be in the future")
    private LocalDate checkOutDate;

    private String notes;

    @Min(value = 1, message = "Debe haber al menos 1 huesped")
    @Max(value = 20, message = "Numero de huespedes invalido")
    private Integer guests;
}
