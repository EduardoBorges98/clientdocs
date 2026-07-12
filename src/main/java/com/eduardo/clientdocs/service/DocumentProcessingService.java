package com.eduardo.clientdocs.service;

import com.eduardo.clientdocs.entity.Client;
import com.eduardo.clientdocs.entity.Document;
import com.eduardo.clientdocs.exception.ResourceNotFoundException;
import com.eduardo.clientdocs.queue.DocumentProcessingMessage;
import com.eduardo.clientdocs.repository.ClientRepository;
import com.eduardo.clientdocs.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Service
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);

    public DocumentProcessingService(
            DocumentRepository documentRepository,
            ClientRepository clientRepository
    ) {
        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
    }

    public void process(DocumentProcessingMessage message) {
        logger.info("Starting document processing. documentId={}, cpfCnpj={}",
                message.getDocumentId(),
                message.getCpfCnpjExtracted()
        );

        Document document = documentRepository.findById(message.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found with id: " + message.getDocumentId()
                ));

        Optional<Client> clientOptional = clientRepository.findByCpfCnpj(
                message.getCpfCnpjExtracted()
        );

        if (clientOptional.isPresent()) {
            document.markAsProcessed(clientOptional.get());

            logger.info("Document processed successfully. documentId={}, clientId={}",
                    document.getId(),
                    clientOptional.get().getId()
            );
        } else {
            document.markAsClientNotFound();

            logger.warn("Document processed but client was not found. documentId={}, cpfCnpj={}",
                    document.getId(),
                    message.getCpfCnpjExtracted()
            );
        }

        documentRepository.save(document);

        logger.info("Document processing result saved. documentId={}, status={}",
                document.getId(),
                document.getStatus()
        );
    }
}