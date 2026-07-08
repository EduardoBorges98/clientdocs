package com.eduardo.clientdocs.storage;

import com.eduardo.clientdocs.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private static final String LOCAL_BUCKET_NAME = "local-storage";
    private static final String ROOT_FOLDER = "storage/local";

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

            Path destinationPath = Path.of(ROOT_FOLDER, storageKey);

            Files.createDirectories(destinationPath.getParent());

            file.transferTo(destinationPath);

            return new StoredFile(
                    LOCAL_BUCKET_NAME,
                    storageKey,
                    file.getContentType(),
                    file.getSize()
            );
        } catch (IOException exception) {
            throw new BusinessException("Failed to store file locally");
        }
    }

    private String sanitizeFileName(String originalFilename) {
        String fileName = Path.of(originalFilename).getFileName().toString();

        return fileName
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .toLowerCase();
    }
}