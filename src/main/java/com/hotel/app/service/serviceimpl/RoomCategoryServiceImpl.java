package com.hotel.app.service.serviceimpl;

import com.hotel.app.dto.request.RoomCategoryRequest;
import com.hotel.app.dto.response.AmenityResponse;
import com.hotel.app.dto.response.RoomCategoryResponse;
import com.hotel.app.model.entity.Amenity;
import com.hotel.app.model.entity.RoomCategory;
import com.hotel.app.exception.BusinessException;
import com.hotel.app.exception.ResourceNotFoundException;
import com.hotel.app.repository.AmenityRepository;
import com.hotel.app.repository.RoomCategoryRepository;
import com.hotel.app.repository.RoomRepository;
import com.hotel.app.service.RoomCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomCategoryServiceImpl implements RoomCategoryService {

    private final RoomCategoryRepository categoryRepository;
    private final AmenityRepository amenityRepository;
    private final RoomRepository roomRepository;

    @Override
    public RoomCategoryResponse createCategory(RoomCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("Ya existe una categoría con este name");
        }

        RoomCategory category = new RoomCategory();
        mapToEntity(request, category);
        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    @Override
    public RoomCategoryResponse updateCategory(Long id, RoomCategoryRequest request) {
        RoomCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        if (!category.getName().equals(request.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("Ya existe una categoría con este name");
        }

        mapToEntity(request, category);
        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    @Override
    public RoomCategoryResponse updateImageUrl(Long id, String imageUrl) {
        RoomCategory category = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));
        category.setImageUrl(imageUrl);
        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    @Override
    public void deleteCategory(Long id) {
        RoomCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        if (roomRepository.existsByCategoryId(id)) {
            throw new BusinessException("No se puede eliminar la categoría porque tiene habitaciones asociadas");
        }

        categoryRepository.delete(category);
    }

    @Override
    public RoomCategoryResponse getCategoryById(Long id) {
        RoomCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        return mapToResponse(category);
    }

    @Override
    public List<RoomCategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void mapToEntity(RoomCategoryRequest request, RoomCategory category) {
        category.setName(request.getName());

        category.setDescription(request.getDescription());

        category.setBasePrice(request.getBasePrice());
        category.setMaxCapacity(request.getMaxCapacity());
        category.setImageUrl(request.getImageUrl());

        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            List<Amenity> amenities = amenityRepository.findAllById(request.getAmenityIds());
            if (amenities.size() != request.getAmenityIds().size()) {
                throw new BusinessException("Una o más amenidades no fueron encontradas");
            }
            category.setAmenities(new HashSet<>(amenities));
        } else {
            category.setAmenities(new HashSet<>());
        }
    }

    private RoomCategoryResponse mapToResponse(RoomCategory category) {
        RoomCategoryResponse response = new RoomCategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setBasePrice(category.getBasePrice());
        response.setMaxCapacity(category.getMaxCapacity());
        response.setImageUrl(category.getImageUrl());
        
        Set<AmenityResponse> amenityResponses = category.getAmenities().stream().map(a -> {
            AmenityResponse ar = new AmenityResponse();
            ar.setId(a.getId());
            ar.setName(a.getName());
            ar.setIcon(a.getIcon());
            return ar;
        }).collect(Collectors.toSet());
        
        response.setAmenities(amenityResponses);
        return response;
    }
}


