package com.hotel.app.service;

import com.hotel.app.dto.request.RoomRequest;
import com.hotel.app.dto.request.RoomStatusUpdateRequest;
import com.hotel.app.dto.response.RoomResponse;
import com.hotel.app.model.enums.RoomStatus;

import java.util.List;

public interface RoomService {
    RoomResponse createRoom(RoomRequest request);
    RoomResponse updateRoom(Long id, RoomRequest request);
    void deleteRoom(Long id);
    RoomResponse getRoomById(Long id);
    List<RoomResponse> getAllRooms(RoomStatus status, Long categoryId);
    RoomResponse updateRoomStatus(Long id, RoomStatusUpdateRequest request);
}
