package com.hotel.app.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DashboardResponse {
    private int totalRooms;
    private int occupiedRooms;
    private int occupancyRate;
    private BigDecimal todayRevenue;
    private long activeReservations;
    private long pendingCheckIns;
}
