package com.eduardo.clientdocs.storage;

import com.eduardo.clientdocs.config.StorageProperties;
import com.eduardo.clientdocs.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    public S3StorageService(S3Client s3Client, StorageProperties storageProperties) {
        this.s3Client = s3Client;
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

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(storageProperties.getS3().getBucketName())
                    .key(storageKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            return new StoredFile(
                    storageProperties.getS3().getBucketName(),
                    storageKey,
                    file.getContentType(),
                    file.getSize()
            );
        } catch (IOException exception) {
            throw new BusinessException("Failed to read file before uploading to S3");
        } catch (S3Exception exception) {
            throw new BusinessException("Failed to upload file to S3: " + exception.awsErrorDetails().errorMessage());
        }
    }

    @Override
    public DownloadedFile download(String storageKey, String originalFileName, String contentType) {
        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(builder -> builder
                .bucket(storageProperties.getS3().getBucketName())
                .key(storageKey)
        );

        return new DownloadedFile(
                objectBytes.asByteArray(),
                originalFileName,
                contentType
        );
    }

    private String sanitizeFileName(String originalFilename) {
        String fileName = Path.of(originalFilename).getFileName().toString();

        return fileName
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .toLowerCase();
    }
}