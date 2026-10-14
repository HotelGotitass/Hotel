package com.hotel.app.service.serviceimpl;

import com.hotel.app.dto.request.UserRequest;
import com.hotel.app.dto.response.UserResponse;
import com.hotel.app.model.entity.User;
import com.hotel.app.model.enums.Role;
import com.hotel.app.exception.BusinessException;
import com.hotel.app.exception.ConflictException;
import com.hotel.app.exception.ResourceNotFoundException;
import com.hotel.app.repository.UserRepository;
import com.hotel.app.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() != Role.CLIENT)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Ya existe un usuario con este correo");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BusinessException("La contrasena es obligatoria para nuevos usuarios");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();
        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
                
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("El correo ya esta en uso por otro usuario");
        }
        
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (currentUsername != null && user.getEmail().equalsIgnoreCase(currentUsername)) {
            throw new BusinessException("No puedes eliminar tu propia cuenta de administrador");
        }

        if (user.getRole() == Role.ADMIN) {
            long adminCount = userRepository.findAll().stream().filter(u -> u.getRole() == Role.ADMIN).count();
            if (adminCount <= 1) {
                throw new BusinessException("No puedes eliminar al ultimo administrador del sistema");
            }
        }

        userRepository.delete(user);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
