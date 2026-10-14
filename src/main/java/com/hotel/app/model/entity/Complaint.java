package com.hotel.app.model.entity;

import com.hotel.app.model.enums.ComplaintStatus;
import com.hotel.app.model.enums.ComplaintType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "document_number", nullable = false)
    private String documentNumber;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "complaint_type", nullable = false)
    private ComplaintType complaintType;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String detail;

    @Column(name = "consumer_request", columnDefinition = "TEXT")
    private String consumerRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
