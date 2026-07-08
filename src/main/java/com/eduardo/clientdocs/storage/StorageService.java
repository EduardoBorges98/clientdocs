package com.eduardo.clientdocs.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    StoredFile store(MultipartFile file);

    DownloadedFile download(String storageKey, String originalFileName, String contentType);
}