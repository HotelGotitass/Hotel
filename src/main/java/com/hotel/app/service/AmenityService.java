package com.hotel.app.service;

import com.hotel.app.dto.request.AmenityRequest;
import com.hotel.app.dto.response.AmenityResponse;

import java.util.List;

public interface AmenityService {
    List<AmenityResponse> getAllAmenities();
    AmenityResponse createAmenity(AmenityRequest request);
}
