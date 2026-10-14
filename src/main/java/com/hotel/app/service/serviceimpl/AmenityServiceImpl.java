package com.hotel.app.service.serviceimpl;

import com.hotel.app.dto.request.AmenityRequest;
import com.hotel.app.dto.response.AmenityResponse;
import com.hotel.app.model.entity.Amenity;
import com.hotel.app.repository.AmenityRepository;
import com.hotel.app.service.AmenityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements AmenityService {

    private final AmenityRepository amenityRepository;

    @Override
    public List<AmenityResponse> getAllAmenities() {
        return amenityRepository.findAll().stream()
                .map(a -> AmenityResponse.builder()
                        .id(a.getId())
                        .name(a.getName())

                        .icon(a.getIcon())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public AmenityResponse createAmenity(AmenityRequest request) {
        Amenity amenity = new Amenity();
        amenity.setName(request.getName());

        amenity.setIcon(request.getIcon());
        amenity = amenityRepository.save(amenity);
        
        return AmenityResponse.builder()
                .id(amenity.getId())
                .name(amenity.getName())
                .icon(amenity.getIcon())
                .build();
    }
}

