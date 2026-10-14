package com.hotel.app.dto.request;

import com.hotel.app.model.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomStatusUpdateRequest {

    @NotNull(message = "El status es requerido")
    private RoomStatus status;
}
