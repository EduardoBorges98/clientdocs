package com.eduardo.clientdocs.service;

import com.eduardo.clientdocs.dto.CreateDocumentRequest;
import com.eduardo.clientdocs.dto.DocumentResponse;
import com.eduardo.clientdocs.entity.Client;
import com.eduardo.clientdocs.entity.Document;
import com.eduardo.clientdocs.enums.DocumentStatus;
import com.eduardo.clientdocs.repository.ClientRepository;
import com.eduardo.clientdocs.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import com.eduardo.clientdocs.exception.BusinessException;
import com.eduardo.clientdocs.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;

    public DocumentService(
            DocumentRepository documentRepository,
            ClientRepository clientRepository
    ) {
        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
    }

    public DocumentResponse createAndProcess(CreateDocumentRequest request) {
        String cpfCnpj = extractCpfCnpjFromFileName(request.getFileName());

        Document document = new Document(
                request.getFileName(),
                cpfCnpj,
                DocumentStatus.PROCESSING
        );

        Optional<Client> clientOptional = clientRepository.findByCpfCnpj(cpfCnpj);

        if (clientOptional.isPresent()) {
            document.markAsProcessed(clientOptional.get());
        } else {
            document.markAsClientNotFound();
        }

        Document savedDocument = documentRepository.save(document);

        return new DocumentResponse(savedDocument);
    }

    public List<DocumentResponse> findAll() {
        return documentRepository.findAll()
                .stream()
                .map(DocumentResponse::new)
                .toList();
    }

    public DocumentResponse findById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));

        return new DocumentResponse(document);
    }

    private String extractCpfCnpjFromFileName(String fileName) {
        String onlyNumbers = fileName.replaceAll("\\D", "");

        if (onlyNumbers.isBlank()) {
            throw new BusinessException("No CPF/CNPJ found in file name: " + fileName);
        }

        return onlyNumbers;
    }
}