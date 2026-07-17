package com.eduardo.clientdocs.controller;

import com.eduardo.clientdocs.dto.DocumentResponse;
import com.eduardo.clientdocs.entity.Document;
import com.eduardo.clientdocs.enums.DocumentStatus;
import com.eduardo.clientdocs.exception.BusinessException;
import com.eduardo.clientdocs.exception.GlobalExceptionHandler;
import com.eduardo.clientdocs.exception.ResourceNotFoundException;
import com.eduardo.clientdocs.service.DocumentService;
import com.eduardo.clientdocs.storage.DownloadedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        DocumentController documentController = new DocumentController(documentService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(documentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldUploadDocumentSuccessfully() throws Exception {
        Document document = createDocument();

        when(documentService.uploadAndProcess(any()))
                .thenReturn(new DocumentResponse(document));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Contrato Cliente 12345678000199.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        mockMvc.perform(multipart("/documents/upload")
                        .file(file)
                        .contentType(MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileName").value("Contrato Cliente 12345678000199.pdf"))
                .andExpect(jsonPath("$.cpfCnpjExtracted").value("12345678000199"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.bucketName").value("clientdocs-eduardo-dev"))
                .andExpect(jsonPath("$.s3Key").value("documents/test.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.fileSize").value(11));
    }

    @Test
    void shouldFindAllDocuments() throws Exception {
        Document document = createDocument();

        when(documentService.findAll())
                .thenReturn(List.of(new DocumentResponse(document)));

        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].fileName").value("Contrato Cliente 12345678000199.pdf"))
                .andExpect(jsonPath("$[0].cpfCnpjExtracted").value("12345678000199"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void shouldFindDocumentById() throws Exception {
        Document document = createDocument();

        when(documentService.findById(1L))
                .thenReturn(new DocumentResponse(document));

        mockMvc.perform(get("/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileName").value("Contrato Cliente 12345678000199.pdf"))
                .andExpect(jsonPath("$.cpfCnpjExtracted").value("12345678000199"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldDownloadDocumentSuccessfully() throws Exception {
        DownloadedFile downloadedFile = new DownloadedFile(
                "pdf content".getBytes(),
                "Contrato Cliente 12345678000199.pdf",
                "application/pdf"
        );

        when(documentService.download(1L))
                .thenReturn(downloadedFile);

        mockMvc.perform(get("/documents/1/download"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"Contrato Cliente 12345678000199.pdf\""
                ))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes("pdf content".getBytes()));
    }

    @Test
    void shouldCreateAndProcessMockDocumentSuccessfully() throws Exception {
        Document document = createDocument();
        document.setStatus(DocumentStatus.PROCESSED);
        document.setProcessedAt(LocalDateTime.of(2026, 7, 17, 19, 0));

        when(documentService.createAndProcess(any()))
                .thenReturn(new DocumentResponse(document));

        String requestBody = """
                {
                  "fileName": "Contrato Cliente 12345678000199.pdf"
                }
                """;

        mockMvc.perform(post("/documents/process-mock")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileName").value("Contrato Cliente 12345678000199.pdf"))
                .andExpect(jsonPath("$.cpfCnpjExtracted").value("12345678000199"))
                .andExpect(jsonPath("$.status").value("PROCESSED"));
    }

    @Test
    void shouldReturnNotFoundWhenDocumentDoesNotExist() throws Exception {
        when(documentService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Document not found with id: 99"));

        mockMvc.perform(get("/documents/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Document not found with id: 99"))
                .andExpect(jsonPath("$.path").value("/documents/99"));
    }

    @Test
    void shouldReturnBadRequestWhenUploadInvalidFile() throws Exception {
        when(documentService.uploadAndProcess(any()))
                .thenThrow(new BusinessException("Only PDF files are allowed"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "text content".getBytes()
        );

        mockMvc.perform(multipart("/documents/upload")
                        .file(file)
                        .contentType(MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Only PDF files are allowed"))
                .andExpect(jsonPath("$.path").value("/documents/upload"));
    }

    @Test
    void shouldReturnBadRequestWhenProcessMockRequestIsInvalid() throws Exception {
        String requestBody = """
                {
                  "fileName": ""
                }
                """;

        mockMvc.perform(post("/documents/process-mock")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("fileName: File name is required")))
                .andExpect(jsonPath("$.path").value("/documents/process-mock"));
    }

    private Document createDocument() {
        Document document = new Document(
                "Contrato Cliente 12345678000199.pdf",
                "12345678000199",
                DocumentStatus.PENDING
        );

        document.setId(1L);
        document.setBucketName("clientdocs-eduardo-dev");
        document.setS3Key("documents/test.pdf");
        document.setContentType("application/pdf");
        document.setFileSize(11L);
        document.setCreatedAt(LocalDateTime.of(2026, 7, 17, 19, 0));

        return document;
    }
}