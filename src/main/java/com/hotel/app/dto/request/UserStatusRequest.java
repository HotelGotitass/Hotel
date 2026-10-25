package com.hotel.app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusRequest {
    @NotNull(message = "Indique si la cuenta queda activa")
    private Boolean active;
}
