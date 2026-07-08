package com.eduardo.clientdocs.controller;

import com.eduardo.clientdocs.dto.CreateDocumentRequest;
import com.eduardo.clientdocs.dto.DocumentResponse;
import com.eduardo.clientdocs.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import com.eduardo.clientdocs.storage.DownloadedFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/process-mock")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse createAndProcess(@RequestBody @Valid CreateDocumentRequest request) {
        return documentService.createAndProcess(request);
    }

    @GetMapping
    public List<DocumentResponse> findAll() {
        return documentService.findAll();
    }

    @GetMapping("/{id}")
    public DocumentResponse findById(@PathVariable Long id) {
        return documentService.findById(id);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadAndProcess(@RequestParam("file") MultipartFile file) {
        return documentService.uploadAndProcess(file);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        DownloadedFile file = documentService.download(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(file.getContent());
    }
}