package com.eduardo.clientdocs.service;

import com.eduardo.clientdocs.dto.ClientResponse;
import com.eduardo.clientdocs.dto.CreateClientRequest;
import com.eduardo.clientdocs.entity.Client;
import com.eduardo.clientdocs.repository.ClientRepository;
import org.springframework.stereotype.Service;
import com.eduardo.clientdocs.exception.BusinessException;
import com.eduardo.clientdocs.exception.ResourceNotFoundException;

import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public ClientResponse create(CreateClientRequest request) {
        boolean cpfCnpjAlreadyExists = clientRepository.existsByCpfCnpj(request.getCpfCnpj());

        if (cpfCnpjAlreadyExists) {
            throw new BusinessException("Client already exists with CPF/CNPJ: " + request.getCpfCnpj());
        }

        Client client = new Client(
                request.getName(),
                request.getCpfCnpj(),
                request.getEmail()
        );

        Client savedClient = clientRepository.save(client);

        return new ClientResponse(savedClient);
    }

    public List<ClientResponse> findAll() {
        return clientRepository.findAll()
                .stream()
                .map(ClientResponse::new)
                .toList();
    }

    public ClientResponse findById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
        return new ClientResponse(client);
    }
}