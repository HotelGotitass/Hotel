package com.hotel.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AmenityRequest {
    @NotBlank
    private String name;

    private String icon;
}

