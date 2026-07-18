package com.eduardo.clientdocs.integration;

import com.eduardo.clientdocs.dto.DocumentResponse;
import com.eduardo.clientdocs.entity.Client;
import com.eduardo.clientdocs.entity.Document;
import com.eduardo.clientdocs.enums.DocumentStatus;
import com.eduardo.clientdocs.queue.DocumentProcessingMessage;
import com.eduardo.clientdocs.queue.DocumentQueueProducer;
import com.eduardo.clientdocs.repository.ClientRepository;
import com.eduardo.clientdocs.repository.DocumentRepository;
import com.eduardo.clientdocs.service.DocumentProcessingService;
import com.eduardo.clientdocs.service.DocumentService;
import com.eduardo.clientdocs.storage.StorageService;
import com.eduardo.clientdocs.storage.StoredFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@Testcontainers
class DocumentFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("clientdocs_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DocumentRepository documentRepository;

    private StorageService storageService;
    private DocumentQueueProducer documentQueueProducer;
    private DocumentService documentService;
    private DocumentProcessingService documentProcessingService;

    @BeforeEach
    void setUp() {
        storageService = mock(StorageService.class);
        documentQueueProducer = mock(DocumentQueueProducer.class);

        documentService = new DocumentService(
                documentRepository,
                clientRepository,
                storageService,
                documentQueueProducer
        );

        documentProcessingService = new DocumentProcessingService(
                documentRepository,
                clientRepository
        );
    }

    @Test
    void shouldUploadDocumentCreatePendingRecordSendQueueMessageAndProcessSuccessfully() {
        Client client = new Client(
                "Test Client",
                "12345678000199",
                "client@test.com"
        );

        Client savedClient = clientRepository.save(client);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Contrato Cliente 12345678000199.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        StoredFile storedFile = new StoredFile(
                "clientdocs-eduardo-dev",
                "documents/test.pdf",
                "application/pdf",
                11L
        );

        when(storageService.store(file))
                .thenReturn(storedFile);

        DocumentResponse uploadResponse = documentService.uploadAndProcess(file);

        assertNotNull(uploadResponse.getId());
        assertEquals("Contrato Cliente 12345678000199.pdf", uploadResponse.getFileName());
        assertEquals("12345678000199", uploadResponse.getCpfCnpjExtracted());
        assertEquals(DocumentStatus.PENDING, uploadResponse.getStatus());
        assertEquals("clientdocs-eduardo-dev", uploadResponse.getBucketName());
        assertEquals("documents/test.pdf", uploadResponse.getS3Key());
        assertEquals("application/pdf", uploadResponse.getContentType());
        assertEquals(11L, uploadResponse.getFileSize());

        ArgumentCaptor<DocumentProcessingMessage> messageCaptor =
                ArgumentCaptor.forClass(DocumentProcessingMessage.class);

        verify(documentQueueProducer).send(messageCaptor.capture());

        DocumentProcessingMessage sentMessage = messageCaptor.getValue();

        assertEquals(uploadResponse.getId(), sentMessage.getDocumentId());
        assertEquals("clientdocs-eduardo-dev", sentMessage.getBucketName());
        assertEquals("documents/test.pdf", sentMessage.getStorageKey());
        assertEquals("12345678000199", sentMessage.getCpfCnpjExtracted());

        Optional<Document> pendingDocumentOptional = documentRepository.findById(uploadResponse.getId());

        assertTrue(pendingDocumentOptional.isPresent());
        assertEquals(DocumentStatus.PENDING, pendingDocumentOptional.get().getStatus());
        assertNull(pendingDocumentOptional.get().getClient());
        assertNull(pendingDocumentOptional.get().getProcessedAt());

        documentProcessingService.process(sentMessage);

        Optional<Document> processedDocumentOptional = documentRepository.findById(uploadResponse.getId());

        assertTrue(processedDocumentOptional.isPresent());

        Document processedDocument = processedDocumentOptional.get();

        assertEquals(DocumentStatus.PROCESSED, processedDocument.getStatus());
        assertNotNull(processedDocument.getClient());
        assertEquals(savedClient.getId(), processedDocument.getClient().getId());
        assertEquals("12345678000199", processedDocument.getClient().getCpfCnpj());
        assertNotNull(processedDocument.getProcessedAt());
    }

    @Test
    void shouldUploadDocumentAndProcessAsClientNotFoundWhenCpfCnpjDoesNotExist() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Contrato Cliente 00000000000000.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        StoredFile storedFile = new StoredFile(
                "clientdocs-eduardo-dev",
                "documents/client-not-found.pdf",
                "application/pdf",
                11L
        );

        when(storageService.store(file))
                .thenReturn(storedFile);

        DocumentResponse uploadResponse = documentService.uploadAndProcess(file);

        ArgumentCaptor<DocumentProcessingMessage> messageCaptor =
                ArgumentCaptor.forClass(DocumentProcessingMessage.class);

        verify(documentQueueProducer).send(messageCaptor.capture());

        DocumentProcessingMessage sentMessage = messageCaptor.getValue();

        documentProcessingService.process(sentMessage);

        Optional<Document> documentOptional = documentRepository.findById(uploadResponse.getId());

        assertTrue(documentOptional.isPresent());

        Document document = documentOptional.get();

        assertEquals(DocumentStatus.CLIENT_NOT_FOUND, document.getStatus());
        assertNull(document.getClient());
        assertNotNull(document.getProcessedAt());
        assertEquals("00000000000000", document.getCpfCnpjExtracted());
    }
}