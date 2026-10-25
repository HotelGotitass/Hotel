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

import java.time.LocalDateTime;
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
    @Transactional(readOnly = true)
    public List<UserResponse> getClientAccounts() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.CLIENT)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse setActive(Long id, boolean active, String currentUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!active) {
            if (currentUsername != null && user.getEmail().equalsIgnoreCase(currentUsername)) {
                throw new BusinessException("No puedes deshabilitar tu propia cuenta");
            }
            if (user.getRole() == Role.ADMIN) {
                long activeAdmins = userRepository.findAll().stream()
                        .filter(u -> u.getRole() == Role.ADMIN && !Boolean.FALSE.equals(u.getActive()))
                        .count();
                if (activeAdmins <= 1) {
                    throw new BusinessException("No puedes deshabilitar al último administrador activo");
                }
            }
            // Cierra todas sus sesiones: los tokens ya emitidos dejan de valer aunque luego se reactive
            user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        }
        user.setActive(active);
        return mapToResponse(userRepository.save(user));
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

    @Override
    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return mapToResponse(userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado")));
    }

    @Override
    @Transactional
    public void changeOwnPassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException("La contraseña actual es incorrecta");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("La nueva contraseña debe ser distinta a la actual");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        // Los clientes recuperan su contrasena por correo; el admin solo gestiona al personal
        if (user.getRole() == Role.CLIENT) {
            throw new BusinessException("Solo se puede cambiar la contraseña del personal del hotel");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userRepository.save(user);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .active(!Boolean.FALSE.equals(user.getActive()))
                .verified(!Boolean.FALSE.equals(user.getIsVerified()))
                .build();
    }
}
