package com.eduardo.clientdocs.queue;

public class DocumentProcessingMessage {

    private Long documentId;
    private String bucketName;
    private String storageKey;
    private String cpfCnpjExtracted;

    public DocumentProcessingMessage() {
    }

    public DocumentProcessingMessage(Long documentId, String bucketName, String storageKey, String cpfCnpjExtracted) {
        this.documentId = documentId;
        this.bucketName = bucketName;
        this.storageKey = storageKey;
        this.cpfCnpjExtracted = cpfCnpjExtracted;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getCpfCnpjExtracted() {
        return cpfCnpjExtracted;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public void setCpfCnpjExtracted(String cpfCnpjExtracted) {
        this.cpfCnpjExtracted = cpfCnpjExtracted;
    }
}