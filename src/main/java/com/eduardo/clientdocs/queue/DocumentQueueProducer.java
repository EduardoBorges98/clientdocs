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

@Component
@ConditionalOnProperty(name = "app.aws.sqs.document-queue-url")
public class DocumentQueueProducer {

    private final SqsClient sqsClient;
    private final AwsProperties awsProperties;
    private final ObjectMapper objectMapper;

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
            String messageBody = objectMapper.writeValueAsString(message);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(awsProperties.getSqs().getDocumentQueueUrl())
                    .messageBody(messageBody)
                    .build();

            sqsClient.sendMessage(request);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Failed to serialize document processing message");
        } catch (SqsException exception) {
            throw new BusinessException("Failed to send message to SQS: " + exception.awsErrorDetails().errorMessage());
        }
    }
}