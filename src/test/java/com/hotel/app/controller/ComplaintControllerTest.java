package com.hotel.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.app.dto.request.ComplaintRequest;
import com.hotel.app.model.enums.ComplaintType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ComplaintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void givenPublicUser_whenSubmitComplaint_thenCreated() throws Exception {
        ComplaintRequest request = ComplaintRequest.builder()
                .fullName("Juan Perez")
                .email("juan.perez@example.com")
                .documentType("DNI")
                .documentNumber("12345678")
                .phone("987654321")
                .complaintType(ComplaintType.RECLAMO)
                .serviceType("Hospedaje Habitación 101")
                .detail("El aire acondicionado no funcionaba en la noche.")
                .consumerRequest("Revisión del cobro o compensación.")
                .build();

        mockMvc.perform(post("/api/v1/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("juan.perez@example.com"))
                .andExpect(jsonPath("$.complaintType").value("RECLAMO"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void givenUnauthenticated_whenGetAllComplaints_thenUnauthorizedOrForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/complaints"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminUser_whenGetAllComplaints_thenOk() throws Exception {
        mockMvc.perform(get("/api/v1/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }
}
