package com.hotel.app.service;

import com.hotel.app.dto.request.LoginRequest;
import com.hotel.app.dto.request.UserRegisterRequest;
import com.hotel.app.dto.response.AuthResponse;
import com.hotel.app.dto.response.UserResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    UserResponse register(UserRegisterRequest request);
}
