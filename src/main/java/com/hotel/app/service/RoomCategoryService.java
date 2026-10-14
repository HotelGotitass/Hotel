package com.hotel.app.service;

import com.hotel.app.dto.request.RoomCategoryRequest;
import com.hotel.app.dto.response.RoomCategoryResponse;

import java.util.List;

public interface RoomCategoryService {
    RoomCategoryResponse createCategory(RoomCategoryRequest request);
    RoomCategoryResponse updateCategory(Long id, RoomCategoryRequest request);
    void deleteCategory(Long id);
    RoomCategoryResponse updateImageUrl(Long id, String imageUrl);
    RoomCategoryResponse getCategoryById(Long id);
    List<RoomCategoryResponse> getAllCategories();
}
