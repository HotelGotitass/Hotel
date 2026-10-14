package com.hotel.app.service.serviceimpl;

import com.hotel.app.dto.request.ClientRegisterRequest;
import com.hotel.app.dto.request.VerifyOtpRequest;
import com.hotel.app.model.entity.Guest;
import com.hotel.app.model.enums.Role;
import com.hotel.app.model.entity.User;
import com.hotel.app.model.entity.VerificationToken;
import com.hotel.app.exception.BusinessException;
import com.hotel.app.exception.ResourceNotFoundException;
import com.hotel.app.repository.GuestRepository;
import com.hotel.app.repository.UserRepository;
import com.hotel.app.repository.VerificationTokenRepository;
import com.hotel.app.service.ClientAuthService;
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
public class ClientAuthServiceImpl implements ClientAuthService {

    private static final int OTP_EXPIRY_MINUTES = 10;

    private final UserRepository userRepository;
    private final GuestRepository guestRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void registerClient(ClientRegisterRequest request) {
        if (!request.isAcceptedTerms() || !request.isAcceptedPrivacy()) {
            throw new BusinessException("Debe aceptar los terminos y politicas de privacidad");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("El email ya esta registrado");
        }

        // Crear User
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CLIENT)
                .isVerified(false)
                .build();
        user = userRepository.save(user);

        // Crear o buscar Guest por documento (decision 20b: Guest atado a User logic)
        Guest guest = guestRepository.findByDocumentNumber(request.getDocumentNumber())
                .orElse(Guest.builder()
                        .documentType(request.getDocumentType())
                        .documentNumber(request.getDocumentNumber())
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .email(request.getEmail())
                        .build());
        
        // Si ya existia, lo actualizamos si hace falta, pero el User es el nuevo
        guestRepository.save(guest);

        // Generar OTP
        String otp = String.format("%06d", secureRandom.nextInt(1000000));
        
        // Guardar token
        VerificationToken token = VerificationToken.builder()
                .user(user)
                .tokenHash(passwordEncoder.encode(otp))
                .expiryDate(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();
        tokenRepository.save(token);

        emailService.sendSimpleEmail(
                user.getEmail(),
                "Código de Verificación - Hotel",
                "Hola " + request.getFirstName() + ",\n\nTu código de verificación de cuenta es: " + otp + "\nExpira en " + OTP_EXPIRY_MINUTES + " minutos."
        );
    }

    @Override
    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (user.getIsVerified()) {
            throw new BusinessException("El usuario ya esta verificado");
        }

        VerificationToken token = tokenRepository.findByUserIdAndPurpose(user.getId(), "ACCOUNT_VERIFICATION")
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

        user.setIsVerified(true);
        userRepository.save(user);
        tokenRepository.delete(token);
    }

    @Override
    @Transactional
    public void forgotPassword(com.hotel.app.dto.request.ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Delete existing reset token if any
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
                "Recuperación de Contraseña - Hotel",
                "Hola,\n\nTu código para restablecer tu contraseña es: " + otp + "\nExpira en " + OTP_EXPIRY_MINUTES + " minutos."
        );
    }

    @Override
    @Transactional
    public void resetPassword(com.hotel.app.dto.request.ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
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


