package com.hotel.app.dto.response;

import com.hotel.app.model.enums.RoomStatus;
import lombok.Data;

@Data
public class RoomResponse {
    private Long id;
    private String roomNumber;
    private Integer floor;
    private RoomCategoryResponse category;
    private RoomStatus status;
}
