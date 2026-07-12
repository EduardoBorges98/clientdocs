package com.eduardo.clientdocs.service;

import com.eduardo.clientdocs.entity.Client;
import com.eduardo.clientdocs.entity.Document;
import com.eduardo.clientdocs.exception.ResourceNotFoundException;
import com.eduardo.clientdocs.queue.DocumentProcessingMessage;
import com.eduardo.clientdocs.repository.ClientRepository;
import com.eduardo.clientdocs.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;

    public DocumentProcessingService(
            DocumentRepository documentRepository,
            ClientRepository clientRepository
    ) {
        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
    }

    public void process(DocumentProcessingMessage message) {
        Document document = documentRepository.findById(message.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found with id: " + message.getDocumentId()
                ));

        Optional<Client> clientOptional = clientRepository.findByCpfCnpj(
                message.getCpfCnpjExtracted()
        );

        if (clientOptional.isPresent()) {
            document.markAsProcessed(clientOptional.get());
        } else {
            document.markAsClientNotFound();
        }

        documentRepository.save(document);
    }
}