package com.eduardo.clientdocs.storage;

import com.eduardo.clientdocs.config.StorageProperties;
import com.eduardo.clientdocs.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
public class LocalStorageService implements StorageService {

    private final StorageProperties storageProperties;

    public LocalStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public StoredFile store(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();

            if (originalFilename == null || originalFilename.isBlank()) {
                throw new BusinessException("File name is required");
            }

            String sanitizedFileName = sanitizeFileName(originalFilename);

            LocalDate today = LocalDate.now();

            String storageKey = "documents/"
                    + today.getYear()
                    + "/"
                    + String.format("%02d", today.getMonthValue())
                    + "/"
                    + UUID.randomUUID()
                    + "-"
                    + sanitizedFileName;

            Path destinationPath = Path.of(
                    storageProperties.getLocal().getRootFolder(),
                    storageKey
            );

            Files.createDirectories(destinationPath.getParent());

            file.transferTo(destinationPath);

            return new StoredFile(
                    storageProperties.getLocal().getBucketName(),
                    storageKey,
                    file.getContentType(),
                    file.getSize()
            );
        } catch (IOException exception) {
            throw new BusinessException("Failed to store file locally");
        }
    }

    @Override
    public DownloadedFile download(String storageKey, String originalFileName, String contentType) {
        try {
            Path filePath = Path.of(
                    storageProperties.getLocal().getRootFolder(),
                    storageKey
            );

            if (!Files.exists(filePath)) {
                throw new BusinessException("Stored file not found");
            }

            byte[] content = Files.readAllBytes(filePath);

            return new DownloadedFile(
                    content,
                    originalFileName,
                    contentType
            );
        } catch (IOException exception) {
            throw new BusinessException("Failed to download stored file");
        }
    }

    private String sanitizeFileName(String originalFilename) {
        String fileName = Path.of(originalFilename).getFileName().toString();

        return fileName
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .toLowerCase();
    }
}