package com.eduardo.clientdocs.storage;

public class StoredFile {

    private String bucketName;
    private String storageKey;
    private String contentType;
    private Long fileSize;

    public StoredFile(String bucketName, String storageKey, String contentType, Long fileSize) {
        this.bucketName = bucketName;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }
}