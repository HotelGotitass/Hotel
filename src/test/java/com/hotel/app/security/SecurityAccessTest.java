package com.hotel.app.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.hotel.app.repository.UserRepository userRepository;

    @Test
    @WithMockUser(roles = "CLIENT")
    void givenClientRole_whenAccessInternalApi_thenForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
                
        mockMvc.perform(get("/api/v1/reservations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminRole_whenAccessInternalApi_thenOk() throws Exception {
        // Users returns OK for ADMIN and excludes CLIENT role
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].role", not(hasItem("CLIENT"))));
    }

    @Test
    @WithMockUser(username = "admin@hotel.com", roles = "ADMIN")
    void givenAdminUser_whenDeleteSelf_thenBadRequest() throws Exception {
        com.hotel.app.model.entity.User admin = userRepository.findByEmail("admin@hotel.com")
                .orElseGet(() -> userRepository.save(com.hotel.app.model.entity.User.builder()
                        .email("admin@hotel.com")
                        .password("hashedPass")
                        .role(com.hotel.app.model.enums.Role.ADMIN)
                        .isVerified(true)
                        .build()));

        mockMvc.perform(delete("/api/v1/users/" + admin.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("No puedes eliminar tu propia cuenta")));
    }
}
