package com.hotel.app.service;

import com.hotel.app.dto.request.GuestRequest;
import com.hotel.app.dto.response.GuestResponse;
import java.util.List;

public interface GuestService {
    GuestResponse createGuest(GuestRequest request);
    GuestResponse getGuestById(Long id);
    List<GuestResponse> searchGuests(String query);
    GuestResponse updateGuest(Long id, GuestRequest request);
}
