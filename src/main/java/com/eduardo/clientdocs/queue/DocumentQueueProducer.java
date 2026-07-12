package com.eduardo.clientdocs.queue;

import com.eduardo.clientdocs.config.AwsProperties;
import com.eduardo.clientdocs.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(name = "app.aws.sqs.document-queue-url")
public class DocumentQueueProducer {

    private final SqsClient sqsClient;
    private final AwsProperties awsProperties;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(DocumentQueueProducer.class);

    public DocumentQueueProducer(
            SqsClient sqsClient,
            AwsProperties awsProperties,
            ObjectMapper objectMapper
    ) {
        this.sqsClient = sqsClient;
        this.awsProperties = awsProperties;
        this.objectMapper = objectMapper;
    }

    public void send(DocumentProcessingMessage message) {
        try {
            logger.info("Sending document processing message to SQS. documentId={}, bucketName={}, storageKey={}",
                    message.getDocumentId(),
                    message.getBucketName(),
                    message.getStorageKey()
            );

            String messageBody = objectMapper.writeValueAsString(message);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(awsProperties.getSqs().getDocumentQueueUrl())
                    .messageBody(messageBody)
                    .build();

            sqsClient.sendMessage(request);

            logger.info("Document processing message sent to SQS successfully. documentId={}",
                    message.getDocumentId()
            );
        } catch (JsonProcessingException exception) {
            logger.error("Failed to serialize document processing message. documentId={}",
                    message.getDocumentId(),
                    exception
            );

            throw new BusinessException("Failed to serialize document processing message");
        } catch (SqsException exception) {
            logger.error("Failed to send document processing message to SQS. documentId={}, awsError={}",
                    message.getDocumentId(),
                    exception.awsErrorDetails().errorMessage(),
                    exception
            );

            throw new BusinessException("Failed to send message to SQS: " + exception.awsErrorDetails().errorMessage());
        }
    }
    }
