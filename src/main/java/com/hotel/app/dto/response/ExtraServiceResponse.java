package com.hotel.app.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ExtraServiceResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Boolean isActive;
}
