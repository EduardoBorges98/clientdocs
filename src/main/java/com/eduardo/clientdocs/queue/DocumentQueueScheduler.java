package com.eduardo.clientdocs.queue;

import com.eduardo.clientdocs.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.aws.sqs.document-queue-url")
public class DocumentQueueScheduler {

    private final DocumentQueueConsumer documentQueueConsumer;

    public DocumentQueueScheduler(DocumentQueueConsumer documentQueueConsumer) {
        this.documentQueueConsumer = documentQueueConsumer;
    }

    @Scheduled(fixedDelayString = "${app.aws.sqs.polling-interval-ms:10000}")
    public void processMessages() {
        try {
            int processedMessages = documentQueueConsumer.processOneMessage();

            if (processedMessages > 0) {
                System.out.println("Processed SQS document messages: " + processedMessages);
            }
        } catch (BusinessException exception) {
            System.out.println("Failed to process SQS document message: " + exception.getMessage());
        }
    }
}