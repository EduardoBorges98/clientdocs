package com.eduardo.clientdocs.service;

import com.eduardo.clientdocs.entity.Client;
import com.eduardo.clientdocs.entity.Document;
import com.eduardo.clientdocs.enums.DocumentStatus;
import com.eduardo.clientdocs.exception.ResourceNotFoundException;
import com.eduardo.clientdocs.queue.DocumentProcessingMessage;
import com.eduardo.clientdocs.repository.ClientRepository;
import com.eduardo.clientdocs.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private DocumentProcessingService documentProcessingService;

    @Test
    void shouldProcessDocumentSuccessfullyWhenClientExists() {
        DocumentProcessingMessage message = new DocumentProcessingMessage(
                1L,
                "clientdocs-eduardo-dev",
                "documents/test.pdf",
                "12345678000199"
        );

        Document document = new Document(
                "Contrato Cliente 12345678000199.pdf",
                "12345678000199",
                DocumentStatus.PENDING
        );
        document.setId(1L);

        Client client = new Client(
                "Test Client",
                "12345678000199",
                "client@test.com"
        );
        client.setId(10L);

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(clientRepository.findByCpfCnpj("12345678000199"))
                .thenReturn(Optional.of(client));

        documentProcessingService.process(message);

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);

        verify(documentRepository).findById(1L);
        verify(clientRepository).findByCpfCnpj("12345678000199");
        verify(documentRepository).save(documentCaptor.capture());

        Document savedDocument = documentCaptor.getValue();

        assertEquals(DocumentStatus.PROCESSED, savedDocument.getStatus());
        assertEquals(client, savedDocument.getClient());
        assertNotNull(savedDocument.getProcessedAt());
    }

    @Test
    void shouldMarkDocumentAsClientNotFoundWhenClientDoesNotExist() {
        DocumentProcessingMessage message = new DocumentProcessingMessage(
                1L,
                "clientdocs-eduardo-dev",
                "documents/test.pdf",
                "99999999000199"
        );

        Document document = new Document(
                "Contrato Cliente 99999999000199.pdf",
                "99999999000199",
                DocumentStatus.PENDING
        );
        document.setId(1L);

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(clientRepository.findByCpfCnpj("99999999000199"))
                .thenReturn(Optional.empty());

        documentProcessingService.process(message);

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);

        verify(documentRepository).findById(1L);
        verify(clientRepository).findByCpfCnpj("99999999000199");
        verify(documentRepository).save(documentCaptor.capture());

        Document savedDocument = documentCaptor.getValue();

        assertEquals(DocumentStatus.CLIENT_NOT_FOUND, savedDocument.getStatus());
        assertNull(savedDocument.getClient());
        assertNotNull(savedDocument.getProcessedAt());
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenDocumentDoesNotExist() {
        DocumentProcessingMessage message = new DocumentProcessingMessage(
                99L,
                "clientdocs-eduardo-dev",
                "documents/missing.pdf",
                "12345678000199"
        );

        when(documentRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> documentProcessingService.process(message)
        );

        assertEquals(
                "Document not found with id: 99",
                exception.getMessage()
        );

        verify(documentRepository).findById(99L);
        verify(clientRepository, never()).findByCpfCnpj(anyString());
        verify(documentRepository, never()).save(any(Document.class));
    }
}