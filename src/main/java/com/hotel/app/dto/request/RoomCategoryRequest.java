package com.hotel.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class RoomCategoryRequest {

    @NotBlank(message = "El name es requerido")
    private String name;

    private String description;



    @NotNull(message = "El precio base es requerido")
    @Min(value = 0, message = "El precio base debe ser mayor o igual a 0")
    private Double basePrice;

    @NotNull(message = "La capacidad máxima es requerida")
    @Min(value = 1, message = "La capacidad máxima debe ser al menos 1")
    private Integer maxCapacity;

    private String imageUrl;

    private Set<Long> amenityIds;
}

