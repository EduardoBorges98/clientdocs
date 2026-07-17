package com.eduardo.clientdocs.queue;

import com.eduardo.clientdocs.config.AwsProperties;
import com.eduardo.clientdocs.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentQueueProducerTest {

    private SqsClient sqsClient;
    private ObjectMapper objectMapper;
    private AwsProperties awsProperties;
    private DocumentQueueProducer documentQueueProducer;

    @BeforeEach
    void setUp() {
        sqsClient = mock(SqsClient.class);
        objectMapper = mock(ObjectMapper.class);

        awsProperties = new AwsProperties();
        awsProperties.getSqs().setDocumentQueueUrl("https://sqs.sa-east-1.amazonaws.com/123/clientdocs-document-processing-dev");

        documentQueueProducer = new DocumentQueueProducer(
                sqsClient,
                awsProperties,
                objectMapper
        );
    }

    @Test
    void shouldSendMessageToSqsSuccessfully() throws Exception {
        DocumentProcessingMessage message = new DocumentProcessingMessage(
                1L,
                "clientdocs-eduardo-dev",
                "documents/test.pdf",
                "12345678000199"
        );

        String messageBody = """
                {
                  "documentId": 1,
                  "bucketName": "clientdocs-eduardo-dev",
                  "storageKey": "documents/test.pdf",
                  "cpfCnpjExtracted": "12345678000199"
                }
                """;

        when(objectMapper.writeValueAsString(message))
                .thenReturn(messageBody);

        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder()
                        .messageId("message-1")
                        .build());

        documentQueueProducer.send(message);

        ArgumentCaptor<SendMessageRequest> requestCaptor =
                ArgumentCaptor.forClass(SendMessageRequest.class);

        verify(objectMapper).writeValueAsString(message);
        verify(sqsClient).sendMessage(requestCaptor.capture());

        SendMessageRequest request = requestCaptor.getValue();

        assertEquals(
                "https://sqs.sa-east-1.amazonaws.com/123/clientdocs-document-processing-dev",
                request.queueUrl()
        );

        assertEquals(messageBody, request.messageBody());
    }

    @Test
    void shouldThrowBusinessExceptionWhenSerializationFails() throws Exception {
        DocumentProcessingMessage message = new DocumentProcessingMessage(
                1L,
                "clientdocs-eduardo-dev",
                "documents/test.pdf",
                "12345678000199"
        );

        when(objectMapper.writeValueAsString(message))
                .thenThrow(new JsonProcessingException("Serialization error") {
                });

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> documentQueueProducer.send(message)
        );

        assertEquals(
                "Failed to serialize document processing message",
                exception.getMessage()
        );

        verify(objectMapper).writeValueAsString(message);
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldThrowBusinessExceptionWhenSqsFails() throws Exception {
        DocumentProcessingMessage message = new DocumentProcessingMessage(
                1L,
                "clientdocs-eduardo-dev",
                "documents/test.pdf",
                "12345678000199"
        );

        String messageBody = """
                {
                  "documentId": 1
                }
                """;

        when(objectMapper.writeValueAsString(message))
                .thenReturn(messageBody);

        SqsException sqsException = mock(SqsException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(sqsException.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorMessage()).thenReturn("SQS is unavailable");

        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(sqsException);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> documentQueueProducer.send(message)
        );

        assertEquals(
                "Failed to send message to SQS: SQS is unavailable",
                exception.getMessage()
        );

        verify(objectMapper).writeValueAsString(message);
        verify(sqsClient).sendMessage(any(SendMessageRequest.class));
    }
}