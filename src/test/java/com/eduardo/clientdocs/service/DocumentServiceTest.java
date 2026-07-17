package com.eduardo.clientdocs.service;

import com.eduardo.clientdocs.dto.DocumentResponse;
import com.eduardo.clientdocs.entity.Document;
import com.eduardo.clientdocs.enums.DocumentStatus;
import com.eduardo.clientdocs.exception.BusinessException;
import com.eduardo.clientdocs.queue.DocumentProcessingMessage;
import com.eduardo.clientdocs.queue.DocumentQueueProducer;
import com.eduardo.clientdocs.repository.ClientRepository;
import com.eduardo.clientdocs.repository.DocumentRepository;
import com.eduardo.clientdocs.storage.DownloadedFile;
import com.eduardo.clientdocs.storage.StorageService;
import com.eduardo.clientdocs.storage.StoredFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private DocumentQueueProducer documentQueueProducer;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void shouldUploadPdfCreatePendingDocumentAndSendMessageToQueue() {
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

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> {
                    Document document = invocation.getArgument(0);
                    document.setId(1L);
                    return document;
                });

        DocumentResponse response = documentService.uploadAndProcess(file);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Contrato Cliente 12345678000199.pdf", response.getFileName());
        assertEquals("12345678000199", response.getCpfCnpjExtracted());
        assertEquals(DocumentStatus.PENDING, response.getStatus());
        assertEquals("clientdocs-eduardo-dev", response.getBucketName());
        assertEquals("documents/test.pdf", response.getS3Key());
        assertEquals("application/pdf", response.getContentType());
        assertEquals(11L, response.getFileSize());

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        ArgumentCaptor<DocumentProcessingMessage> messageCaptor =
                ArgumentCaptor.forClass(DocumentProcessingMessage.class);

        verify(storageService).store(file);
        verify(documentRepository).save(documentCaptor.capture());
        verify(documentQueueProducer).send(messageCaptor.capture());

        Document savedDocument = documentCaptor.getValue();

        assertEquals("Contrato Cliente 12345678000199.pdf", savedDocument.getFileName());
        assertEquals("12345678000199", savedDocument.getCpfCnpjExtracted());
        assertEquals(DocumentStatus.PENDING, savedDocument.getStatus());
        assertEquals("clientdocs-eduardo-dev", savedDocument.getBucketName());
        assertEquals("documents/test.pdf", savedDocument.getS3Key());
        assertEquals("application/pdf", savedDocument.getContentType());
        assertEquals(11L, savedDocument.getFileSize());

        DocumentProcessingMessage sentMessage = messageCaptor.getValue();

        assertEquals(1L, sentMessage.getDocumentId());
        assertEquals("clientdocs-eduardo-dev", sentMessage.getBucketName());
        assertEquals("documents/test.pdf", sentMessage.getStorageKey());
        assertEquals("12345678000199", sentMessage.getCpfCnpjExtracted());
    }

    @Test
    void shouldThrowBusinessExceptionWhenFileIsNull() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> documentService.uploadAndProcess(null)
        );

        assertEquals("File is required", exception.getMessage());

        verify(storageService, never()).store(any());
        verify(documentRepository, never()).save(any());
        verify(documentQueueProducer, never()).send(any());
    }

    @Test
    void shouldThrowBusinessExceptionWhenFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> documentService.uploadAndProcess(file)
        );

        assertEquals("File is required", exception.getMessage());

        verify(storageService, never()).store(any());
        verify(documentRepository, never()).save(any());
        verify(documentQueueProducer, never()).send(any());
    }

    @Test
    void shouldThrowBusinessExceptionWhenFileNameIsMissing() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "",
                "application/pdf",
                "pdf content".getBytes()
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> documentService.uploadAndProcess(file)
        );

        assertEquals("File name is required", exception.getMessage());

        verify(storageService, never()).store(any());
        verify(documentRepository, never()).save(any());
        verify(documentQueueProducer, never()).send(any());
    }

    @Test
    void shouldThrowBusinessExceptionWhenFileIsNotPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "text content".getBytes()
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> documentService.uploadAndProcess(file)
        );

        assertEquals("Only PDF files are allowed", exception.getMessage());

        verify(storageService, never()).store(any());
        verify(documentRepository, never()).save(any());
        verify(documentQueueProducer, never()).send(any());
    }

    @Test
    void shouldThrowBusinessExceptionWhenPdfFileNameHasNoCpfCnpj() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Contrato Cliente Final.pdf",
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

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> documentService.uploadAndProcess(file)
        );

        assertEquals(
                "No CPF/CNPJ found in file name: Contrato Cliente Final.pdf",
                exception.getMessage()
        );

        verify(storageService).store(file);
        verify(documentRepository, never()).save(any());
        verify(documentQueueProducer, never()).send(any());
    }

    @Test
    void shouldDownloadDocumentSuccessfully() {
        Document document = new Document(
                "Contrato Cliente 12345678000199.pdf",
                "12345678000199",
                DocumentStatus.PROCESSED
        );
        document.setId(1L);
        document.setS3Key("documents/test.pdf");
        document.setContentType("application/pdf");

        DownloadedFile downloadedFile = new DownloadedFile(
                "pdf content".getBytes(),
                "Contrato Cliente 12345678000199.pdf",
                "application/pdf"
        );

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        when(storageService.download(
                "documents/test.pdf",
                "Contrato Cliente 12345678000199.pdf",
                "application/pdf"
        )).thenReturn(downloadedFile);

        DownloadedFile response = documentService.download(1L);

        assertNotNull(response);
        assertEquals("Contrato Cliente 12345678000199.pdf", response.getFileName());
        assertEquals("application/pdf", response.getContentType());
        assertArrayEquals("pdf content".getBytes(), response.getContent());

        verify(documentRepository).findById(1L);
        verify(storageService).download(
                "documents/test.pdf",
                "Contrato Cliente 12345678000199.pdf",
                "application/pdf"
        );
    }
}