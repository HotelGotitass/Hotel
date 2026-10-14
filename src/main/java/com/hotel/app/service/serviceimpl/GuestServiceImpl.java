package com.hotel.app.service.serviceimpl;

import com.hotel.app.dto.request.GuestRequest;
import com.hotel.app.dto.response.GuestResponse;
import com.hotel.app.model.entity.Guest;
import com.hotel.app.exception.ConflictException;
import com.hotel.app.exception.ResourceNotFoundException;
import com.hotel.app.repository.GuestRepository;
import com.hotel.app.service.GuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuestServiceImpl implements GuestService {

    private final GuestRepository guestRepository;

    @Override
    public GuestResponse createGuest(GuestRequest request) {
        if (guestRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new ConflictException("Ya existe un huesped con el documento " + request.getDocumentNumber());
        }

        Guest guest = Guest.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();

        return mapToResponse(guestRepository.save(guest));
    }

    @Override
    public GuestResponse getGuestById(Long id) {
        Guest guest = guestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Huesped no encontrado con ID: " + id));
        return mapToResponse(guest);
    }

    @Override
    public List<GuestResponse> searchGuests(String query) {
        if (query == null || query.trim().isEmpty()) {
            return guestRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
        }
        return guestRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public GuestResponse updateGuest(Long id, GuestRequest request) {
        Guest guest = guestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Huesped no encontrado con ID: " + id));

        if (!guest.getDocumentNumber().equals(request.getDocumentNumber()) && 
            guestRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new ConflictException("Ya existe otro huesped con el documento " + request.getDocumentNumber());
        }

        guest.setFirstName(request.getFirstName());
        guest.setLastName(request.getLastName());
        guest.setDocumentType(request.getDocumentType());
        guest.setDocumentNumber(request.getDocumentNumber());
        guest.setEmail(request.getEmail());
        guest.setPhone(request.getPhone());
        guest.setAddress(request.getAddress());

        return mapToResponse(guestRepository.save(guest));
    }

    private GuestResponse mapToResponse(Guest guest) {
        return GuestResponse.builder()
                .id(guest.getId())
                .firstName(guest.getFirstName())
                .lastName(guest.getLastName())
                .documentType(guest.getDocumentType())
                .documentNumber(guest.getDocumentNumber())
                .email(guest.getEmail())
                .phone(guest.getPhone())
                .address(guest.getAddress())
                .createdAt(guest.getCreatedAt())
                .build();
    }
}
