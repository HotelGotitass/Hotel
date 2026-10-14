package com.hotel.app.service.serviceimpl;

import com.hotel.app.dto.request.LoginRequest;
import com.hotel.app.dto.request.UserRegisterRequest;
import com.hotel.app.dto.response.AuthResponse;
import com.hotel.app.dto.response.UserResponse;
import com.hotel.app.model.entity.User;
import com.hotel.app.exception.BusinessException;
import com.hotel.app.repository.UserRepository;
import com.hotel.app.security.JwtUtil;
import com.hotel.app.security.UserDetailsImpl;
import com.hotel.app.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtil.generateJwtToken(authentication);
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return AuthResponse.builder()
                .token(jwt)
                .role(userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""))
                .build();
    }

    @Override
    public UserResponse register(UserRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("El email ya está en uso");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        userRepository.save(user);

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
