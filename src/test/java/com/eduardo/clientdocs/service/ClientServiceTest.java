package com.eduardo.clientdocs.service;

import com.eduardo.clientdocs.dto.ClientResponse;
import com.eduardo.clientdocs.dto.CreateClientRequest;
import com.eduardo.clientdocs.entity.Client;
import com.eduardo.clientdocs.exception.BusinessException;
import com.eduardo.clientdocs.exception.ResourceNotFoundException;
import com.eduardo.clientdocs.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    @Test
    void shouldCreateClientWhenCpfCnpjDoesNotExist() {
        CreateClientRequest request = new CreateClientRequest();
        request.setName("Test Client");
        request.setCpfCnpj("12345678000199");
        request.setEmail("client@test.com");

        Client savedClient = new Client(
                "Test Client",
                "12345678000199",
                "client@test.com"
        );

        when(clientRepository.existsByCpfCnpj("12345678000199"))
                .thenReturn(false);

        when(clientRepository.save(any(Client.class)))
                .thenReturn(savedClient);

        ClientResponse response = clientService.create(request);

        assertNotNull(response);
        assertEquals("Test Client", response.getName());
        assertEquals("12345678000199", response.getCpfCnpj());
        assertEquals("client@test.com", response.getEmail());

        verify(clientRepository).existsByCpfCnpj("12345678000199");
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void shouldThrowBusinessExceptionWhenCpfCnpjAlreadyExists() {
        CreateClientRequest request = new CreateClientRequest();
        request.setName("Test Client");
        request.setCpfCnpj("12345678000199");
        request.setEmail("client@test.com");

        when(clientRepository.existsByCpfCnpj("12345678000199"))
                .thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> clientService.create(request)
        );

        assertEquals(
                "Client already exists with CPF/CNPJ: 12345678000199",
                exception.getMessage()
        );

        verify(clientRepository).existsByCpfCnpj("12345678000199");
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void shouldFindAllClients() {
        Client client = new Client(
                "Test Client",
                "12345678000199",
                "client@test.com"
        );

        when(clientRepository.findAll())
                .thenReturn(List.of(client));

        List<ClientResponse> response = clientService.findAll();

        assertEquals(1, response.size());
        assertEquals("Test Client", response.get(0).getName());
        assertEquals("12345678000199", response.get(0).getCpfCnpj());
        assertEquals("client@test.com", response.get(0).getEmail());

        verify(clientRepository).findAll();
    }

    @Test
    void shouldFindClientById() {
        Client client = new Client(
                "Test Client",
                "12345678000199",
                "client@test.com"
        );

        when(clientRepository.findById(1L))
                .thenReturn(Optional.of(client));

        ClientResponse response = clientService.findById(1L);

        assertNotNull(response);
        assertEquals("Test Client", response.getName());
        assertEquals("12345678000199", response.getCpfCnpj());
        assertEquals("client@test.com", response.getEmail());

        verify(clientRepository).findById(1L);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenClientDoesNotExist() {
        when(clientRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> clientService.findById(99L)
        );

        assertEquals(
                "Client not found with id: 99",
                exception.getMessage()
        );

        verify(clientRepository).findById(99L);
    }
}