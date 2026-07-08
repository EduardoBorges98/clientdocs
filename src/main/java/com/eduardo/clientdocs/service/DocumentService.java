package com.eduardo.clientdocs.service;

import com.eduardo.clientdocs.dto.CreateDocumentRequest;
import com.eduardo.clientdocs.dto.DocumentResponse;
import com.eduardo.clientdocs.entity.Client;
import com.eduardo.clientdocs.entity.Document;
import com.eduardo.clientdocs.enums.DocumentStatus;
import com.eduardo.clientdocs.repository.ClientRepository;
import com.eduardo.clientdocs.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import com.eduardo.clientdocs.exception.BusinessException;
import com.eduardo.clientdocs.exception.ResourceNotFoundException;
import org.springframework.web.multipart.MultipartFile;
import com.eduardo.clientdocs.storage.StorageService;
import com.eduardo.clientdocs.storage.StoredFile;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final StorageService storageService;

    public DocumentService(
            DocumentRepository documentRepository,
            ClientRepository clientRepository,
            StorageService storageService
    ) {
        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
        this.storageService = storageService;
    }

    public DocumentResponse createAndProcess(CreateDocumentRequest request) {
        String cpfCnpj = extractCpfCnpjFromFileName(request.getFileName());

        Document document = new Document(
                request.getFileName(),
                cpfCnpj,
                DocumentStatus.PROCESSING
        );

        Optional<Client> clientOptional = clientRepository.findByCpfCnpj(cpfCnpj);

        if (clientOptional.isPresent()) {
            document.markAsProcessed(clientOptional.get());
        } else {
            document.markAsClientNotFound();
        }

        Document savedDocument = documentRepository.save(document);

        return new DocumentResponse(savedDocument);
    }

    public List<DocumentResponse> findAll() {
        return documentRepository.findAll()
                .stream()
                .map(DocumentResponse::new)
                .toList();
    }

    public DocumentResponse findById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + id));

        return new DocumentResponse(document);
    }

    private String extractCpfCnpjFromFileName(String fileName) {
        String onlyNumbers = fileName.replaceAll("\\D", "");

        if (onlyNumbers.isBlank()) {
            throw new BusinessException("No CPF/CNPJ found in file name: " + fileName);
        }

        return onlyNumbers;
    }

    public DocumentResponse uploadAndProcess(MultipartFile file) {
        validatePdfFile(file);
        StoredFile storedFile = storageService.store(file);

        String fileName = file.getOriginalFilename();
        String cpfCnpj = extractCpfCnpjFromFileName(fileName);

        Document document = new Document(
                fileName,
                cpfCnpj,
                DocumentStatus.PROCESSING
        );

        document.setBucketName(storedFile.getBucketName());
        document.setS3Key(storedFile.getStorageKey());
        document.setContentType(storedFile.getContentType());
        document.setFileSize(storedFile.getFileSize());

        Optional<Client> clientOptional = clientRepository.findByCpfCnpj(cpfCnpj);

        if (clientOptional.isPresent()) {
            document.markAsProcessed(clientOptional.get());
        } else {
            document.markAsClientNotFound();
        }

        Document savedDocument = documentRepository.save(document);

        return new DocumentResponse(savedDocument);
    }

    private void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is required");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("File name is required");
        }

        boolean hasPdfExtension = originalFilename.toLowerCase().endsWith(".pdf");
        boolean hasPdfContentType = "application/pdf".equalsIgnoreCase(file.getContentType());

        if (!hasPdfExtension || !hasPdfContentType) {
            throw new BusinessException("Only PDF files are allowed");
        }
    }
}