package com.hotel.app.service;

import com.hotel.app.dto.request.ComplaintRequest;
import com.hotel.app.dto.response.ComplaintResponse;
import com.hotel.app.model.enums.ComplaintStatus;

import java.util.List;

public interface ComplaintService {
    ComplaintResponse createComplaint(ComplaintRequest request);
    List<ComplaintResponse> getAllComplaints();
    ComplaintResponse updateStatus(Long id, ComplaintStatus status);
}
