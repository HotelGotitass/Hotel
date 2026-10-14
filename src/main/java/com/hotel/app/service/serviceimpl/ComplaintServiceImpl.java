package com.hotel.app.service.serviceimpl;

import com.hotel.app.dto.request.ComplaintRequest;
import com.hotel.app.dto.response.ComplaintResponse;
import com.hotel.app.exception.ResourceNotFoundException;
import com.hotel.app.model.entity.Complaint;
import com.hotel.app.model.enums.ComplaintStatus;
import com.hotel.app.repository.ComplaintRepository;
import com.hotel.app.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;

    @Override
    @Transactional
    public ComplaintResponse createComplaint(ComplaintRequest request) {
        Complaint complaint = Complaint.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .documentType(request.getDocumentType().trim())
                .documentNumber(request.getDocumentNumber().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .complaintType(request.getComplaintType())
                .serviceType(request.getServiceType().trim())
                .detail(request.getDetail().trim())
                .consumerRequest(request.getConsumerRequest() != null ? request.getConsumerRequest().trim() : null)
                .status(ComplaintStatus.PENDING)
                .build();

        Complaint saved = complaintRepository.save(complaint);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ComplaintResponse updateStatus(Long id, ComplaintStatus status) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reclamación no encontrada con id: " + id));

        complaint.setStatus(status);
        return mapToResponse(complaintRepository.save(complaint));
    }

    private ComplaintResponse mapToResponse(Complaint complaint) {
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .fullName(complaint.getFullName())
                .email(complaint.getEmail())
                .documentType(complaint.getDocumentType())
                .documentNumber(complaint.getDocumentNumber())
                .phone(complaint.getPhone())
                .complaintType(complaint.getComplaintType())
                .serviceType(complaint.getServiceType())
                .detail(complaint.getDetail())
                .consumerRequest(complaint.getConsumerRequest())
                .status(complaint.getStatus())
                .createdAt(complaint.getCreatedAt())
                .build();
    }
}
