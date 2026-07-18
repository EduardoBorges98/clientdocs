package com.eduardo.clientdocs.repository;

import com.eduardo.clientdocs.entity.Client;
import com.eduardo.clientdocs.entity.Document;
import com.eduardo.clientdocs.enums.DocumentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class DocumentRepositoryTest {

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
    private DocumentRepository documentRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void shouldPersistDocumentWithPendingStatus() {
        Document document = new Document(
                "Contrato Cliente 12345678000199.pdf",
                "12345678000199",
                DocumentStatus.PENDING
        );

        document.setBucketName("clientdocs-eduardo-dev");
        document.setS3Key("documents/test.pdf");
        document.setContentType("application/pdf");
        document.setFileSize(11L);

        Document savedDocument = documentRepository.save(document);

        assertNotNull(savedDocument.getId());
        assertEquals("Contrato Cliente 12345678000199.pdf", savedDocument.getFileName());
        assertEquals("12345678000199", savedDocument.getCpfCnpjExtracted());
        assertEquals(DocumentStatus.PENDING, savedDocument.getStatus());
        assertEquals("clientdocs-eduardo-dev", savedDocument.getBucketName());
        assertEquals("documents/test.pdf", savedDocument.getS3Key());
        assertEquals("application/pdf", savedDocument.getContentType());
        assertEquals(11L, savedDocument.getFileSize());
        assertNull(savedDocument.getClient());
        assertNull(savedDocument.getProcessedAt());
    }

    @Test
    void shouldFindDocumentsByStatus() {
        Document pendingDocument = new Document(
                "Pendente 12345678000199.pdf",
                "12345678000199",
                DocumentStatus.PENDING
        );

        Document processedDocument = new Document(
                "Processado 98765432000199.pdf",
                "98765432000199",
                DocumentStatus.PROCESSED
        );

        documentRepository.save(pendingDocument);
        documentRepository.save(processedDocument);

        List<Document> pendingDocuments = documentRepository.findByStatus(DocumentStatus.PENDING);

        assertEquals(1, pendingDocuments.size());
        assertEquals("Pendente 12345678000199.pdf", pendingDocuments.get(0).getFileName());
        assertEquals(DocumentStatus.PENDING, pendingDocuments.get(0).getStatus());
    }

    @Test
    void shouldPersistProcessedDocumentWithClient() {
        Client client = new Client(
                "Test Client",
                "12345678000199",
                "client@test.com"
        );

        Client savedClient = clientRepository.save(client);

        Document document = new Document(
                "Contrato Cliente 12345678000199.pdf",
                "12345678000199",
                DocumentStatus.PENDING
        );

        document.markAsProcessed(savedClient);

        Document savedDocument = documentRepository.save(document);

        assertNotNull(savedDocument.getId());
        assertEquals(DocumentStatus.PROCESSED, savedDocument.getStatus());
        assertNotNull(savedDocument.getClient());
        assertEquals(savedClient.getId(), savedDocument.getClient().getId());
        assertNotNull(savedDocument.getProcessedAt());
    }

    @Test
    void shouldPersistClientNotFoundStatus() {
        Document document = new Document(
                "Contrato Cliente 00000000000000.pdf",
                "00000000000000",
                DocumentStatus.PENDING
        );

        document.markAsClientNotFound();

        Document savedDocument = documentRepository.save(document);

        assertNotNull(savedDocument.getId());
        assertEquals(DocumentStatus.CLIENT_NOT_FOUND, savedDocument.getStatus());
        assertNull(savedDocument.getClient());
        assertNotNull(savedDocument.getProcessedAt());
    }
}