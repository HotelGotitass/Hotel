package com.hotel.app.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "invoice_series")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSeries {

    @Id
    @Column(length = 4)
    private String series;

    @Column(name = "last_number", nullable = false)
    private Integer lastNumber;
}
