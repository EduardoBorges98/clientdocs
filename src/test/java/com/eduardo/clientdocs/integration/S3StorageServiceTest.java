package com.eduardo.clientdocs.storage;

import com.eduardo.clientdocs.config.StorageProperties;
import com.eduardo.clientdocs.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class S3StorageServiceTest {

    private S3Client s3Client;
    private StorageProperties storageProperties;
    private S3StorageService s3StorageService;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        storageProperties = mock(StorageProperties.class, RETURNS_DEEP_STUBS);

        when(storageProperties.getS3().getBucketName())
                .thenReturn("clientdocs-eduardo-dev");

        s3StorageService = new S3StorageService(
                s3Client,
                storageProperties
        );
    }

    @Test
    void shouldStoreFileInS3Successfully() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Contrato Cliente 12345678000199.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        StoredFile storedFile = s3StorageService.store(file);

        ArgumentCaptor<PutObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client).putObject(
                requestCaptor.capture(),
                any(RequestBody.class)
        );

        PutObjectRequest request = requestCaptor.getValue();

        assertEquals("clientdocs-eduardo-dev", request.bucket());
        assertTrue(request.key().startsWith("documents/"));
        assertTrue(request.key().endsWith("-contrato_cliente_12345678000199.pdf"));
        assertEquals("application/pdf", request.contentType());
        assertEquals(11L, request.contentLength());

        assertEquals("clientdocs-eduardo-dev", storedFile.getBucketName());
        assertEquals(request.key(), storedFile.getStorageKey());
        assertEquals("application/pdf", storedFile.getContentType());
        assertEquals(11L, storedFile.getFileSize());
    }

    @Test
    void shouldThrowBusinessExceptionWhenFileNameIsMissing() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "",
                "application/pdf",
                "pdf content".getBytes()
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> s3StorageService.store(file)
        );

        assertEquals("File name is required", exception.getMessage());

        verify(s3Client, never()).putObject(
                any(PutObjectRequest.class),
                any(RequestBody.class)
        );
    }

    @Test
    void shouldThrowBusinessExceptionWhenFileInputStreamFails() throws Exception {
        MockMultipartFile file = spy(new MockMultipartFile(
                "file",
                "Contrato Cliente 12345678000199.pdf",
                "application/pdf",
                "pdf content".getBytes()
        ));

        doThrow(new IOException("InputStream failed"))
                .when(file)
                .getInputStream();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> s3StorageService.store(file)
        );

        assertEquals("Failed to read file before uploading to S3", exception.getMessage());

        verify(s3Client, never()).putObject(
                any(PutObjectRequest.class),
                any(RequestBody.class)
        );
    }

    @Test
    void shouldThrowBusinessExceptionWhenS3UploadFails() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Contrato Cliente 12345678000199.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        S3Exception s3Exception = mock(S3Exception.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(s3Exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorMessage()).thenReturn("Access denied");

        doThrow(s3Exception)
                .when(s3Client)
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> s3StorageService.store(file)
        );

        assertEquals("Failed to upload file to S3: Access denied", exception.getMessage());
    }

    @Test
    void shouldDownloadFileFromS3Successfully() {
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(),
                "pdf content".getBytes()
        );

        when(s3Client.getObjectAsBytes(any(java.util.function.Consumer.class)))
                .thenReturn(responseBytes);

        DownloadedFile downloadedFile = s3StorageService.download(
                "documents/test.pdf",
                "Contrato Cliente 12345678000199.pdf",
                "application/pdf"
        );

        assertArrayEquals("pdf content".getBytes(), downloadedFile.getContent());
        assertEquals("Contrato Cliente 12345678000199.pdf", downloadedFile.getFileName());
        assertEquals("application/pdf", downloadedFile.getContentType());

        verify(s3Client).getObjectAsBytes(any(java.util.function.Consumer.class));
    }
}