package com.eduardo.clientdocs.queue;

import com.eduardo.clientdocs.config.AwsProperties;
import com.eduardo.clientdocs.exception.BusinessException;
import com.eduardo.clientdocs.service.DocumentProcessingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentQueueConsumerTest {

    private SqsClient sqsClient;
    private ObjectMapper objectMapper;
    private DocumentProcessingService documentProcessingService;
    private AwsProperties awsProperties;
    private DocumentQueueConsumer documentQueueConsumer;

    @BeforeEach
    void setUp() {
        sqsClient = mock(SqsClient.class);
        objectMapper = mock(ObjectMapper.class);
        documentProcessingService = mock(DocumentProcessingService.class);

        awsProperties = new AwsProperties();
        awsProperties.getSqs().setDocumentQueueUrl("https://sqs.sa-east-1.amazonaws.com/123/clientdocs-document-processing-dev");

        documentQueueConsumer = new DocumentQueueConsumer(
                sqsClient,
                awsProperties,
                objectMapper,
                documentProcessingService
        );
    }

    @Test
    void shouldReturnZeroWhenQueueHasNoMessages() throws Exception {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder()
                        .messages(List.of())
                        .build());

        int result = documentQueueConsumer.processOneMessage();

        assertEquals(0, result);

        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
        verify(objectMapper, never()).readValue(anyString(), eq(DocumentProcessingMessage.class));
        verify(documentProcessingService, never()).process(any(DocumentProcessingMessage.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldProcessAndDeleteMessageWhenProcessingSucceeds() throws Exception {
        String body = """
                {
                  "documentId": 1,
                  "bucketName": "clientdocs-eduardo-dev",
                  "storageKey": "documents/test.pdf",
                  "cpfCnpjExtracted": "12345678000199"
                }
                """;

        Message sqsMessage = Message.builder()
                .messageId("message-1")
                .receiptHandle("receipt-1")
                .body(body)
                .attributes(Map.of(
                        MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT,
                        "2"
                ))
                .build();

        DocumentProcessingMessage documentMessage = new DocumentProcessingMessage(
                1L,
                "clientdocs-eduardo-dev",
                "documents/test.pdf",
                "12345678000199"
        );

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder()
                        .messages(List.of(sqsMessage))
                        .build());

        when(objectMapper.readValue(body, DocumentProcessingMessage.class))
                .thenReturn(documentMessage);

        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(DeleteMessageResponse.builder().build());

        int result = documentQueueConsumer.processOneMessage();

        assertEquals(1, result);

        ArgumentCaptor<ReceiveMessageRequest> receiveRequestCaptor =
                ArgumentCaptor.forClass(ReceiveMessageRequest.class);

        ArgumentCaptor<DeleteMessageRequest> deleteRequestCaptor =
                ArgumentCaptor.forClass(DeleteMessageRequest.class);

        verify(sqsClient).receiveMessage(receiveRequestCaptor.capture());
        verify(objectMapper).readValue(body, DocumentProcessingMessage.class);
        verify(documentProcessingService).process(documentMessage);
        verify(sqsClient).deleteMessage(deleteRequestCaptor.capture());

        ReceiveMessageRequest receiveRequest = receiveRequestCaptor.getValue();

        assertEquals(
                "https://sqs.sa-east-1.amazonaws.com/123/clientdocs-document-processing-dev",
                receiveRequest.queueUrl()
        );
        assertEquals(1, receiveRequest.maxNumberOfMessages());
        assertEquals(5, receiveRequest.waitTimeSeconds());
        assertTrue(receiveRequest.messageSystemAttributeNames()
                .contains(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT));

        DeleteMessageRequest deleteRequest = deleteRequestCaptor.getValue();

        assertEquals(
                "https://sqs.sa-east-1.amazonaws.com/123/clientdocs-document-processing-dev",
                deleteRequest.queueUrl()
        );
        assertEquals("receipt-1", deleteRequest.receiptHandle());
    }

    @Test
    void shouldThrowBusinessExceptionWhenMessageParsingFails() throws Exception {
        String invalidBody = """
                {
                  "invalid": true
                }
                """;

        Message sqsMessage = Message.builder()
                .messageId("message-1")
                .receiptHandle("receipt-1")
                .body(invalidBody)
                .attributes(Map.of(
                        MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT,
                        "1"
                ))
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder()
                        .messages(List.of(sqsMessage))
                        .build());

        when(objectMapper.readValue(invalidBody, DocumentProcessingMessage.class))
                .thenThrow(new JsonProcessingException("Invalid JSON") {
                });

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> documentQueueConsumer.processOneMessage()
        );

        assertEquals(
                "Failed to parse SQS document processing message",
                exception.getMessage()
        );

        verify(documentProcessingService, never()).process(any(DocumentProcessingMessage.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldNotDeleteMessageWhenProcessingFails() throws Exception {
        String body = """
                {
                  "documentId": 1,
                  "bucketName": "clientdocs-eduardo-dev",
                  "storageKey": "documents/test.pdf",
                  "cpfCnpjExtracted": "12345678000199"
                }
                """;

        Message sqsMessage = Message.builder()
                .messageId("message-1")
                .receiptHandle("receipt-1")
                .body(body)
                .attributes(Map.of(
                        MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT,
                        "3"
                ))
                .build();

        DocumentProcessingMessage documentMessage = new DocumentProcessingMessage(
                1L,
                "clientdocs-eduardo-dev",
                "documents/test.pdf",
                "12345678000199"
        );

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder()
                        .messages(List.of(sqsMessage))
                        .build());

        when(objectMapper.readValue(body, DocumentProcessingMessage.class))
                .thenReturn(documentMessage);

        doThrow(new RuntimeException("Processing failed"))
                .when(documentProcessingService)
                .process(documentMessage);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> documentQueueConsumer.processOneMessage()
        );

        assertEquals("Processing failed", exception.getMessage());

        verify(documentProcessingService).process(documentMessage);
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldThrowBusinessExceptionWhenSqsReceiveFails() {
        SqsException sqsException = mock(SqsException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(sqsException.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorMessage()).thenReturn("SQS receive failed");

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenThrow(sqsException);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> documentQueueConsumer.processOneMessage()
        );

        assertEquals(
                "Failed to consume message from SQS: SQS receive failed",
                exception.getMessage()
        );

        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }
}