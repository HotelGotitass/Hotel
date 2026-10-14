package com.hotel.app.dto.response;

import lombok.Data;
import java.util.Set;

@Data
public class RoomCategoryResponse {
    private Long id;
    private String name;
    private String description;


    private Double basePrice;
    private Integer maxCapacity;

    private String imageUrl;
    private Set<AmenityResponse> amenities;
}

