package com.hotel.app.service;

import com.hotel.app.dto.request.ChangePasswordConfirmRequest;
import com.hotel.app.dto.request.ClientProfileUpdateRequest;

public interface ClientProfileService {
    void updateProfile(Long userId, ClientProfileUpdateRequest request);
    void requestPasswordChange(Long userId);
    void changePassword(Long userId, ChangePasswordConfirmRequest request);
}
