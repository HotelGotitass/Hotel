package com.hotel.app.dto.request;

import com.hotel.app.model.enums.RoomStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomRequest {

    @NotBlank(message = "El número de habitación es requerido")
    private String roomNumber;

    @NotNull(message = "El floor es requerido")
    private Integer floor;

    @NotNull(message = "La categoría es requerida")
    private Long categoryId;

    @NotNull(message = "El status es requerido")
    private RoomStatus status;
}
