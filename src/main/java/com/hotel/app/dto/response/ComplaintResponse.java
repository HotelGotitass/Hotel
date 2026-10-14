package com.hotel.app.dto.response;

import com.hotel.app.model.enums.ComplaintStatus;
import com.hotel.app.model.enums.ComplaintType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintResponse {

    private Long id;
    private String fullName;
    private String email;
    private String documentType;
    private String documentNumber;
    private String phone;
    private ComplaintType complaintType;
    private String serviceType;
    private String detail;
    private String consumerRequest;
    private ComplaintStatus status;
    private LocalDateTime createdAt;
}
