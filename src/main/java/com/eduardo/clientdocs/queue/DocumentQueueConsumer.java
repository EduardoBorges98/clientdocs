package com.eduardo.clientdocs.queue;

import com.eduardo.clientdocs.config.AwsProperties;
import com.eduardo.clientdocs.exception.BusinessException;
import com.eduardo.clientdocs.service.DocumentProcessingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.aws.sqs.document-queue-url")
public class DocumentQueueConsumer {

    private final SqsClient sqsClient;
    private final AwsProperties awsProperties;
    private final ObjectMapper objectMapper;
    private final DocumentProcessingService documentProcessingService;
    private static final Logger logger = LoggerFactory.getLogger(DocumentQueueConsumer.class);

    public DocumentQueueConsumer(
            SqsClient sqsClient,
            AwsProperties awsProperties,
            ObjectMapper objectMapper,
            DocumentProcessingService documentProcessingService
    ) {
        this.sqsClient = sqsClient;
        this.awsProperties = awsProperties;
        this.objectMapper = objectMapper;
        this.documentProcessingService = documentProcessingService;
    }

    public int processOneMessage() {
        try {
            logger.debug("Polling SQS document queue for messages");

            ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                    .queueUrl(awsProperties.getSqs().getDocumentQueueUrl())
                    .maxNumberOfMessages(1)
                    .waitTimeSeconds(5)
                    .build();

            List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();

            if (messages.isEmpty()) {
                logger.debug("No SQS document messages available");
                return 0;
            }

            Message message = messages.get(0);

            logger.info("Received SQS document message. messageId={}", message.messageId());

            DocumentProcessingMessage documentMessage = objectMapper.readValue(
                    message.body(),
                    DocumentProcessingMessage.class
            );

            logger.info("Processing SQS document message. documentId={}", documentMessage.getDocumentId());

            documentProcessingService.process(documentMessage);

            DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                    .queueUrl(awsProperties.getSqs().getDocumentQueueUrl())
                    .receiptHandle(message.receiptHandle())
                    .build();

            sqsClient.deleteMessage(deleteMessageRequest);

            logger.info("SQS document message processed and deleted. documentId={}, messageId={}",
                    documentMessage.getDocumentId(),
                    message.messageId()
            );

            return 1;
        } catch (JsonProcessingException exception) {
            logger.error("Failed to parse SQS document processing message", exception);
            throw new BusinessException("Failed to parse SQS document processing message");
        } catch (SqsException exception) {
            logger.error("Failed to consume message from SQS. awsError={}",
                    exception.awsErrorDetails().errorMessage(),
                    exception
            );

            throw new BusinessException("Failed to consume message from SQS: " + exception.awsErrorDetails().errorMessage());
        }
    }
}