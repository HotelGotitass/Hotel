package com.hotel.app.controller;

import com.hotel.app.dto.response.RoomCategoryResponse;
import com.hotel.app.model.enums.ReservationStatus;
import com.hotel.app.model.entity.RoomCategory;
import com.hotel.app.model.enums.RoomStatus;
import com.hotel.app.repository.ReservationRepository;
import com.hotel.app.repository.RoomCategoryRepository;
import com.hotel.app.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicSearchController {

    private final RoomCategoryRepository categoryRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    @GetMapping("/search")
    public ResponseEntity<List<RoomCategoryResponse>> searchAvailableCategories(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") Integer capacity) {

        List<RoomCategory> categories = categoryRepository.findAll();
        List<RoomCategoryResponse> availableCategories = new ArrayList<>();

        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);

        for (RoomCategory cat : categories) {
            if (cat.getMaxCapacity() >= capacity) {
                long totalRooms = roomRepository.countByCategoryIdAndStatus(cat.getId(), RoomStatus.AVAILABLE);
                long overlapping = reservationRepository.countOverlappingReservations(cat.getId(), checkIn, checkOut, activeStatuses);

                if (totalRooms - overlapping > 0) {
                    availableCategories.add(mapToResponse(cat));
                }
            }
        }

        return ResponseEntity.ok(availableCategories);
    }

    private RoomCategoryResponse mapToResponse(RoomCategory category) {
        RoomCategoryResponse res = new RoomCategoryResponse();
        res.setId(category.getId());
        res.setName(category.getName());
        res.setDescription(category.getDescription());
        res.setBasePrice(category.getBasePrice().doubleValue());
        res.setMaxCapacity(category.getMaxCapacity());
        res.setImageUrl(category.getImageUrl());
        return res;
    }
}
