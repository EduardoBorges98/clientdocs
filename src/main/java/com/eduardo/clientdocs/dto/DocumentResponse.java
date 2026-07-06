package com.eduardo.clientdocs.dto;

import com.eduardo.clientdocs.entity.Document;
import com.eduardo.clientdocs.enums.DocumentStatus;

import java.time.LocalDateTime;

public class DocumentResponse {

    private Long id;
    private String fileName;
    private String cpfCnpjExtracted;
    private DocumentStatus status;
    private Long clientId;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public DocumentResponse(Document document) {
        this.id = document.getId();
        this.fileName = document.getFileName();
        this.cpfCnpjExtracted = document.getCpfCnpjExtracted();
        this.status = document.getStatus();
        this.clientId = document.getClient() != null ? document.getClient().getId() : null;
        this.createdAt = document.getCreatedAt();
        this.processedAt = document.getProcessedAt();
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getCpfCnpjExtracted() {
        return cpfCnpjExtracted;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public Long getClientId() {
        return clientId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}