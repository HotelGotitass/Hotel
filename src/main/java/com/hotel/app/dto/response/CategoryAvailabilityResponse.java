package com.hotel.app.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CategoryAvailabilityResponse {
    private Long categoryId;
    private String name;
    private String description;
    private Double basePrice;
    private Integer maxCapacity;
    private String imageUrl;
    private long availableRooms;
    private java.util.List<AmenityResponse> amenities;
}
