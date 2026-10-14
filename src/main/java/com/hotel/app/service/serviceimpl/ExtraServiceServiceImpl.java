package com.hotel.app.service.serviceimpl;

import com.hotel.app.dto.request.ExtraServiceRequest;
import com.hotel.app.dto.response.ExtraServiceResponse;
import com.hotel.app.model.entity.ExtraService;
import com.hotel.app.exception.ConflictException;
import com.hotel.app.exception.ResourceNotFoundException;
import com.hotel.app.repository.ExtraServiceRepository;
import com.hotel.app.service.ExtraServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExtraServiceServiceImpl implements ExtraServiceService {

    private final ExtraServiceRepository repository;

    @Override
    @Transactional
    public ExtraServiceResponse createService(ExtraServiceRequest request) {
        if (repository.existsByName(request.getName())) {
            throw new ConflictException("Ya existe un servicio con el nombre " + request.getName());
        }

        ExtraService service = ExtraService.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return mapToResponse(repository.save(service));
    }

    @Override
    @Transactional
    public ExtraServiceResponse updateService(Long id, ExtraServiceRequest request) {
        ExtraService service = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));

        if (!service.getName().equals(request.getName()) && repository.existsByName(request.getName())) {
            throw new ConflictException("Ya existe otro servicio con el nombre " + request.getName());
        }

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setPrice(request.getPrice());
        if (request.getIsActive() != null) {
            service.setIsActive(request.getIsActive());
        }

        return mapToResponse(repository.save(service));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExtraServiceResponse> getAllServices() {
        return repository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExtraServiceResponse> getActiveServices() {
        return repository.findByIsActiveTrue().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private ExtraServiceResponse mapToResponse(ExtraService service) {
        return ExtraServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .isActive(service.getIsActive())
                .build();
    }
}
