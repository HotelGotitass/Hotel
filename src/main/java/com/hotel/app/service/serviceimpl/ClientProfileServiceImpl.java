package com.hotel.app.service.serviceimpl;

import com.hotel.app.dto.request.ChangePasswordConfirmRequest;
import com.hotel.app.dto.request.ClientProfileUpdateRequest;
import com.hotel.app.model.entity.Guest;
import com.hotel.app.model.entity.User;
import com.hotel.app.model.entity.VerificationToken;
import com.hotel.app.exception.BusinessException;
import com.hotel.app.exception.ResourceNotFoundException;
import com.hotel.app.repository.GuestRepository;
import com.hotel.app.repository.UserRepository;
import com.hotel.app.repository.VerificationTokenRepository;
import com.hotel.app.service.ClientProfileService;
import com.hotel.app.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientProfileServiceImpl implements ClientProfileService {

    private static final int OTP_EXPIRY_MINUTES = 10;

    private final UserRepository userRepository;
    private final GuestRepository guestRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void updateProfile(Long userId, ClientProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        Guest guest = guestRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Guest no encontrado"));
                
        guest.setFirstName(request.getFirstName());
        guest.setLastName(request.getLastName());
        guestRepository.save(guest);
    }

    @Override
    @Transactional
    public void requestPasswordChange(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        tokenRepository.findByUserId(user.getId()).ifPresent(t -> { tokenRepository.delete(t); tokenRepository.flush(); });

        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        
        VerificationToken token = VerificationToken.builder()
                .user(user)
                .tokenHash(passwordEncoder.encode(otp))
                .expiryDate(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .purpose("PASSWORD_RESET")
                .build();
        tokenRepository.save(token);

        emailService.sendSimpleEmail(
                user.getEmail(),
                "Cambio de Contraseña - Hotel",
                "Hola,\n\nTu código para confirmar el cambio de contraseña es: " + otp + "\nExpira en " + OTP_EXPIRY_MINUTES + " minutos."
        );
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordConfirmRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        VerificationToken token = tokenRepository.findByUserIdAndPurpose(user.getId(), "PASSWORD_RESET")
                .orElseThrow(() -> new BusinessException("No hay codigo pendiente para este usuario"));

        if (token.getAttempts() >= 5) {
            throw new BusinessException("Demasiados intentos fallidos. Solicite un nuevo codigo.");
        }

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException("El codigo ha expirado");
        }

        if (!passwordEncoder.matches(request.getOtp(), token.getTokenHash())) {
            token.setAttempts(token.getAttempts() + 1);
            tokenRepository.save(token);
            throw new BusinessException("Codigo incorrecto");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        tokenRepository.delete(token);
    }
}


