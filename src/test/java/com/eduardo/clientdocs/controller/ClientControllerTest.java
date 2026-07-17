package com.eduardo.clientdocs.controller;

import com.eduardo.clientdocs.dto.ClientResponse;
import com.eduardo.clientdocs.entity.Client;
import com.eduardo.clientdocs.exception.BusinessException;
import com.eduardo.clientdocs.exception.GlobalExceptionHandler;
import com.eduardo.clientdocs.exception.ResourceNotFoundException;
import com.eduardo.clientdocs.service.ClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private ClientService clientService;

    @BeforeEach
    void setUp() {
        ClientController clientController = new ClientController(clientService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(clientController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldCreateClient() throws Exception {
        Client client = createClient();

        when(clientService.create(any()))
                .thenReturn(new ClientResponse(client));

        String requestBody = """
                {
                  "name": "Test Client",
                  "cpfCnpj": "12345678000199",
                  "email": "client@test.com"
                }
                """;

        mockMvc.perform(post("/clients")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Client"))
                .andExpect(jsonPath("$.cpfCnpj").value("12345678000199"))
                .andExpect(jsonPath("$.email").value("client@test.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldFindAllClients() throws Exception {
        Client client = createClient();

        when(clientService.findAll())
                .thenReturn(List.of(new ClientResponse(client)));

        mockMvc.perform(get("/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Test Client"))
                .andExpect(jsonPath("$[0].cpfCnpj").value("12345678000199"))
                .andExpect(jsonPath("$[0].email").value("client@test.com"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void shouldFindClientById() throws Exception {
        Client client = createClient();

        when(clientService.findById(1L))
                .thenReturn(new ClientResponse(client));

        mockMvc.perform(get("/clients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Client"))
                .andExpect(jsonPath("$.cpfCnpj").value("12345678000199"))
                .andExpect(jsonPath("$.email").value("client@test.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturnBadRequestWhenClientAlreadyExists() throws Exception {
        when(clientService.create(any()))
                .thenThrow(new BusinessException("Client already exists with CPF/CNPJ: 12345678000199"));

        String requestBody = """
                {
                  "name": "Test Client",
                  "cpfCnpj": "12345678000199",
                  "email": "client@test.com"
                }
                """;

        mockMvc.perform(post("/clients")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Client already exists with CPF/CNPJ: 12345678000199"))
                .andExpect(jsonPath("$.path").value("/clients"));
    }

    @Test
    void shouldReturnNotFoundWhenClientDoesNotExist() throws Exception {
        when(clientService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Client not found with id: 99"));

        mockMvc.perform(get("/clients/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Client not found with id: 99"))
                .andExpect(jsonPath("$.path").value("/clients/99"));
    }

    @Test
    void shouldReturnBadRequestWhenCreateClientRequestIsInvalid() throws Exception {
        String requestBody = """
                {
                  "name": "",
                  "cpfCnpj": "",
                  "email": "invalid-email"
                }
                """;

        mockMvc.perform(post("/clients")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("name: Name is required")))
                .andExpect(jsonPath("$.message", containsString("cpfCnpj: CPF/CNPJ is required")))
                .andExpect(jsonPath("$.message", containsString("email: Email must be valid")))
                .andExpect(jsonPath("$.path").value("/clients"));
    }

    private Client createClient() {
        Client client = new Client(
                "Test Client",
                "12345678000199",
                "client@test.com"
        );

        client.setId(1L);
        client.setActive(true);
        client.setCreatedAt(LocalDateTime.of(2026, 7, 17, 19, 0));
        client.setUpdatedAt(null);

        return client;
    }
}