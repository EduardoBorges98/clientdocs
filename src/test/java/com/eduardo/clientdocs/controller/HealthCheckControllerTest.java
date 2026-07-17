package com.eduardo.clientdocs.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HealthCheckControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HealthCheckController healthCheckController = new HealthCheckController();

        mockMvc = MockMvcBuilders
                .standaloneSetup(healthCheckController)
                .build();
    }

    @Test
    void shouldReturnApiInfo() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("ClientDocs Processor"))
                .andExpect(jsonPath("$.status").value("running"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.health").value("/health"))
                .andExpect(jsonPath("$.docs").value("/swagger-ui/index.html"));
    }

    @Test
    void shouldReturnHealthStatusUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("clientdocs-api"));
    }
}