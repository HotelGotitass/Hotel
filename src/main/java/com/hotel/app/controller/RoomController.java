package com.hotel.app.controller;

import com.hotel.app.dto.request.RoomRequest;
import com.hotel.app.dto.request.RoomStatusUpdateRequest;
import com.hotel.app.dto.response.RoomResponse;
import com.hotel.app.model.enums.RoomStatus;
import com.hotel.app.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomRequest request) {
        return new ResponseEntity<>(roomService.createRoom(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRooms(
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(roomService.getAllRooms(status, categoryId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomResponse> updateRoomStatus(
            @PathVariable Long id,
            @Valid @RequestBody RoomStatusUpdateRequest request) {
        return ResponseEntity.ok(roomService.updateRoomStatus(id, request));
    }
}
