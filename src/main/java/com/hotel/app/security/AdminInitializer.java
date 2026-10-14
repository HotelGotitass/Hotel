package com.hotel.app.security;

import com.hotel.app.model.enums.Role;
import com.hotel.app.model.entity.User;
import com.hotel.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = System.getenv("ADMIN_EMAIL");
        String adminPassword = System.getenv("ADMIN_PASSWORD");

        if (adminEmail == null || adminEmail.trim().isEmpty() || adminPassword == null || adminPassword.trim().isEmpty()) {
            log.warn("=========================================================================");
            log.warn("Variables de entorno ADMIN_EMAIL y/o ADMIN_PASSWORD no encontradas.");
            log.warn("Omitiendo la creacion automatica del primer administrador.");
            log.warn("=========================================================================");
            return;
        }

        // Check if any admin already exists
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Ya existe al menos un usuario administrador en la base de datos.");
            return;
        }

        log.info("No se encontro ningun administrador. Creando el primer ADMIN desde variables de entorno...");

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);

        userRepository.save(admin);
        
        log.info("Primer usuario ADMIN creado con exito con el correo: {}", adminEmail);
    }
}
