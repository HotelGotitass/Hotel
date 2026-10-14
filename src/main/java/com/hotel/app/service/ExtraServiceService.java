package com.hotel.app.service;

import com.hotel.app.dto.request.ExtraServiceRequest;
import com.hotel.app.dto.response.ExtraServiceResponse;
import java.util.List;

public interface ExtraServiceService {
    ExtraServiceResponse createService(ExtraServiceRequest request);
    ExtraServiceResponse updateService(Long id, ExtraServiceRequest request);
    List<ExtraServiceResponse> getAllServices();
    List<ExtraServiceResponse> getActiveServices();
}
