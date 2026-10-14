package com.hotel.app.service.serviceimpl;

import com.hotel.app.dto.request.RoomRequest;
import com.hotel.app.dto.request.RoomStatusUpdateRequest;
import com.hotel.app.dto.response.AmenityResponse;
import com.hotel.app.dto.response.RoomCategoryResponse;
import com.hotel.app.dto.response.RoomResponse;
import com.hotel.app.model.entity.Room;
import com.hotel.app.model.entity.RoomCategory;
import com.hotel.app.model.enums.RoomStatus;
import com.hotel.app.exception.BusinessException;
import com.hotel.app.exception.ResourceNotFoundException;
import com.hotel.app.repository.RoomCategoryRepository;
import com.hotel.app.repository.RoomRepository;
import com.hotel.app.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomCategoryRepository categoryRepository;

    @Override
    public RoomResponse createRoom(RoomRequest request) {
        if (roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new BusinessException("El número de habitación ya existe");
        }

        RoomCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        Room room = new Room();
        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor());
        room.setCategory(category);
        room.setStatus(request.getStatus());

        room = roomRepository.save(room);
        return mapToResponse(room);
    }

    @Override
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habitación no encontrada"));

        if (!room.getRoomNumber().equals(request.getRoomNumber()) && roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new BusinessException("El número de habitación ya existe");
        }

        RoomCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor());
        room.setCategory(category);
        room.setStatus(request.getStatus());

        room = roomRepository.save(room);
        return mapToResponse(room);
    }

    @Override
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habitación no encontrada"));
        
        // TODO: En futuros sprints, validar que la habitación no tenga reservas
        // if (reservationRepository.existsByRoomId(id)) {
        //     throw new BusinessException("No se puede eliminar la habitación porque tiene reservas asociadas");
        // }

        roomRepository.delete(room);
    }

    @Override
    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habitación no encontrada"));
        return mapToResponse(room);
    }

    @Override
    public List<RoomResponse> getAllRooms(RoomStatus status, Long categoryId) {
        return roomRepository.findByEstadoAndCategory(status, categoryId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RoomResponse updateRoomStatus(Long id, RoomStatusUpdateRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habitación no encontrada"));
        
        room.setStatus(request.getStatus());
        room = roomRepository.save(room);
        return mapToResponse(room);
    }

    private RoomResponse mapToResponse(Room room) {
        RoomResponse response = new RoomResponse();
        response.setId(room.getId());
        response.setRoomNumber(room.getRoomNumber());
        response.setFloor(room.getFloor());
        response.setStatus(room.getStatus());

        RoomCategoryResponse categoryResponse = new RoomCategoryResponse();
        categoryResponse.setId(room.getCategory().getId());
        categoryResponse.setName(room.getCategory().getName());
        categoryResponse.setDescription(room.getCategory().getDescription());
        categoryResponse.setBasePrice(room.getCategory().getBasePrice());
        categoryResponse.setMaxCapacity(room.getCategory().getMaxCapacity());
        
        Set<AmenityResponse> amenityResponses = room.getCategory().getAmenities().stream().map(a -> {
            AmenityResponse ar = new AmenityResponse();
            ar.setId(a.getId());
            ar.setName(a.getName());
            ar.setIcon(a.getIcon());
            return ar;
        }).collect(Collectors.toSet());
        
        categoryResponse.setAmenities(amenityResponses);
        response.setCategory(categoryResponse);

        return response;
    }
}
