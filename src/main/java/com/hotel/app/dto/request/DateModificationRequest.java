package com.hotel.app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class DateModificationRequest {
    @NotNull(message = "La nueva fecha de check-in es obligatoria")
    private LocalDate newCheckIn;
    
    @NotNull(message = "La nueva fecha de check-out es obligatoria")
    private LocalDate newCheckOut;
}
