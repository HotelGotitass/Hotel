package com.hotel.app.service;

import com.hotel.app.dto.request.ClientRegisterRequest;
import com.hotel.app.dto.request.ForgotPasswordRequest;
import com.hotel.app.model.entity.User;
import com.hotel.app.model.entity.VerificationToken;
import com.hotel.app.repository.GuestRepository;
import com.hotel.app.repository.UserRepository;
import com.hotel.app.repository.VerificationTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.mail.username=test@example.com",
        "spring.mail.password=dummy"
})
@ExtendWith(OutputCaptureExtension.class)
class EmailNotificationIntegrationTest {

    @MockBean
    private JavaMailSender mailSender;

    @Autowired
    private ClientAuthService clientAuthService;

    @Autowired
    private ClientProfileService clientProfileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final List<String> createdEmails = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String email : createdEmails) {
            try {
                userRepository.findByEmail(email).ifPresent(user -> {
                    List<VerificationToken> tokens = tokenRepository.findAll().stream()
                            .filter(t -> t.getUser() != null && t.getUser().getId().equals(user.getId()))
                            .toList();
                    tokenRepository.deleteAll(tokens);
                    userRepository.delete(user);
                });
                guestRepository.findByEmail(email).ifPresent(guestRepository::delete);
            } catch (Exception ignored) {
                // Un fallo en la limpieza de un email no impide limpiar los siguientes
            }
        }
        createdEmails.clear();
    }

    @Test
    void givenClientRegistration_whenCommitted_thenEmailSentAndOtpNotLogged(CapturedOutput output) {
        String uniqueEmail = "client-" + UUID.randomUUID() + "@example.com";
        createdEmails.add(uniqueEmail);

        ClientRegisterRequest request = new ClientRegisterRequest();
        request.setEmail(uniqueEmail);
        request.setPassword("Password123!");
        request.setFirstName("Carlos");
        request.setLastName("Gomez");
        request.setDocumentType("DNI");
        request.setDocumentNumber(UUID.randomUUID().toString().substring(0, 8));
        request.setAcceptedTerms(true);
        request.setAcceptedPrivacy(true);

        // Sin @Transactional en el método de test: commit real en PostgreSQL al retornar el servicio
        clientAuthService.registerClient(request);

        // Verificar que el correo se despacha tras el commit
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(2000)).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertThat(sentMessage.getTo()).contains(uniqueEmail);
        assertThat(sentMessage.getSubject()).isEqualTo("Código de Verificación - Hotel");

        // Extraer OTP del cuerpo del mensaje
        String body = sentMessage.getText();
        assertThat(body).isNotNull();
        Matcher matcher = Pattern.compile("\\b\\d{6}\\b").matcher(body);
        assertThat(matcher.find()).isTrue();
        String extractedOtp = matcher.group();

        // Verificar con límites de dígitos que el OTP extraído no aparece en los logs
        Pattern otpBoundaryPattern = Pattern.compile("(?<!\\d)" + Pattern.quote(extractedOtp) + "(?!\\d)");
        assertThat(otpBoundaryPattern.matcher(output.getAll()).find()).isFalse();
        assertThat(output.getAll()).doesNotContain("SIMULACION DE CORREO");
    }

    @Test
    void givenClientRegistration_whenRollbackForced_thenEmailNeverSent() {
        String uniqueEmail = "client-rollback-" + UUID.randomUUID() + "@example.com";
        createdEmails.add(uniqueEmail);

        ClientRegisterRequest request = new ClientRegisterRequest();
        request.setEmail(uniqueEmail);
        request.setPassword("Password123!");
        request.setFirstName("Maria");
        request.setLastName("Lopez");
        request.setDocumentType("DNI");
        request.setDocumentNumber(UUID.randomUUID().toString().substring(0, 8));
        request.setAcceptedTerms(true);
        request.setAcceptedPrivacy(true);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            transactionTemplate.execute(status -> {
                clientAuthService.registerClient(request);
                // Forzar rollback explícito de la transacción en PostgreSQL
                throw new RuntimeException("Rollback forzado para verificar que no se envíe correo");
            });
        });

        // Verificar que la excepción es exactamente la forzada y no un error previo de registerClient
        assertThat(thrownException.getMessage()).isEqualTo("Rollback forzado para verificar que no se envíe correo");

        // Verificar que tras el rollback NO se despacha el correo
        verify(mailSender, after(1000).never()).send(any(SimpleMailMessage.class));

        // Verificar que en PostgreSQL no quedó persistido
        assertThat(userRepository.findByEmail(uniqueEmail)).isEmpty();
    }

    @Test
    void givenForgotPassword_whenCommitted_thenEmailSentAndOtpNotLogged(CapturedOutput output) {
        String uniqueEmail = "forgot-" + UUID.randomUUID() + "@example.com";
        createdEmails.add(uniqueEmail);

        ClientRegisterRequest registerRequest = new ClientRegisterRequest();
        registerRequest.setEmail(uniqueEmail);
        registerRequest.setPassword("Password123!");
        registerRequest.setFirstName("Ana");
        registerRequest.setLastName("Torres");
        registerRequest.setDocumentType("DNI");
        registerRequest.setDocumentNumber(UUID.randomUUID().toString().substring(0, 8));
        registerRequest.setAcceptedTerms(true);
        registerRequest.setAcceptedPrivacy(true);

        clientAuthService.registerClient(registerRequest);
        verify(mailSender, timeout(2000)).send(any(SimpleMailMessage.class));
        reset(mailSender);

        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest();
        forgotRequest.setEmail(uniqueEmail);

        clientAuthService.forgotPassword(forgotRequest);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(2000)).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertThat(sentMessage.getTo()).contains(uniqueEmail);
        assertThat(sentMessage.getSubject()).isEqualTo("Recuperación de Contraseña - Hotel");

        // Extraer OTP y verificar que no aparece en logs con límites de dígito
        String body = sentMessage.getText();
        assertThat(body).isNotNull();
        Matcher matcher = Pattern.compile("\\b\\d{6}\\b").matcher(body);
        assertThat(matcher.find()).isTrue();
        String extractedOtp = matcher.group();

        Pattern otpBoundaryPattern = Pattern.compile("(?<!\\d)" + Pattern.quote(extractedOtp) + "(?!\\d)");
        assertThat(otpBoundaryPattern.matcher(output.getAll()).find()).isFalse();
        assertThat(output.getAll()).doesNotContain("SIMULACION DE CORREO");
    }

    @Test
    void givenRequestPasswordChange_whenCommitted_thenEmailSentAndOtpNotLogged(CapturedOutput output) {
        String uniqueEmail = "profile-" + UUID.randomUUID() + "@example.com";
        createdEmails.add(uniqueEmail);

        ClientRegisterRequest registerRequest = new ClientRegisterRequest();
        registerRequest.setEmail(uniqueEmail);
        registerRequest.setPassword("Password123!");
        registerRequest.setFirstName("Pedro");
        registerRequest.setLastName("Suarez");
        registerRequest.setDocumentType("DNI");
        registerRequest.setDocumentNumber(UUID.randomUUID().toString().substring(0, 8));
        registerRequest.setAcceptedTerms(true);
        registerRequest.setAcceptedPrivacy(true);

        clientAuthService.registerClient(registerRequest);
        verify(mailSender, timeout(2000)).send(any(SimpleMailMessage.class));
        reset(mailSender);

        User user = userRepository.findByEmail(uniqueEmail).orElseThrow();
        clientProfileService.requestPasswordChange(user.getId());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(2000)).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertThat(sentMessage.getTo()).contains(uniqueEmail);
        assertThat(sentMessage.getSubject()).isEqualTo("Cambio de Contraseña - Hotel");

        // Extraer OTP y verificar que no aparece en logs con límites de dígito
        String body = sentMessage.getText();
        assertThat(body).isNotNull();
        Matcher matcher = Pattern.compile("\\b\\d{6}\\b").matcher(body);
        assertThat(matcher.find()).isTrue();
        String extractedOtp = matcher.group();

        Pattern otpBoundaryPattern = Pattern.compile("(?<!\\d)" + Pattern.quote(extractedOtp) + "(?!\\d)");
        assertThat(otpBoundaryPattern.matcher(output.getAll()).find()).isFalse();
        assertThat(output.getAll()).doesNotContain("SIMULACION DE CORREO");
    }
}
