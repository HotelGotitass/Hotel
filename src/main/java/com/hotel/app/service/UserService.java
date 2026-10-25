package com.hotel.app.service;

import com.hotel.app.dto.request.UserRequest;
import com.hotel.app.dto.response.UserResponse;
import java.util.List;

public interface UserService {
    List<UserResponse> getAllUsers();
    List<UserResponse> getClientAccounts();
    UserResponse setActive(Long id, boolean active, String currentUsername);
    UserResponse createUser(UserRequest request);
    UserResponse updateUser(Long id, UserRequest request);
    void deleteUser(Long id, String currentUsername);
    UserResponse getByEmail(String email);
    void changeOwnPassword(String email, String currentPassword, String newPassword);
    void resetPassword(Long id, String newPassword);
}
