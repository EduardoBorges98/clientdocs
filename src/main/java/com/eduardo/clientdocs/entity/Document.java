package com.eduardo.clientdocs.entity;

import com.eduardo.clientdocs.enums.DocumentStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "cpf_cnpj_extracted", length = 20)
    private String cpfCnpjExtracted;

    @Column(name = "bucket_name", length = 100)
    private String bucketName;

    @Column(name = "s3_key", length = 500)
    private String s3Key;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DocumentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;


    public Document() {
    }

    public Document(String fileName, String cpfCnpjExtracted, DocumentStatus status) {
        this.fileName = fileName;
        this.cpfCnpjExtracted = cpfCnpjExtracted;
        this.status = status;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = DocumentStatus.PENDING;
        }
    }

    public void markAsProcessing() {
        this.status = DocumentStatus.PROCESSING;
    }

    public void markAsProcessed(Client client) {
        this.client = client;
        this.status = DocumentStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsClientNotFound() {
        this.status = DocumentStatus.CLIENT_NOT_FOUND;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsError() {
        this.status = DocumentStatus.ERROR;
        this.processedAt = LocalDateTime.now();
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

    public Client getClient() {
        return client;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setCpfCnpjExtracted(String cpfCnpjExtracted) {
        this.cpfCnpjExtracted = cpfCnpjExtracted;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
}