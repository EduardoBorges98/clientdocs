package com.eduardo.clientdocs.queue;

import com.eduardo.clientdocs.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.aws.sqs.document-queue-url")
public class DocumentQueueScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DocumentQueueScheduler.class);

    private final DocumentQueueConsumer documentQueueConsumer;

    public DocumentQueueScheduler(DocumentQueueConsumer documentQueueConsumer) {
        this.documentQueueConsumer = documentQueueConsumer;
    }

    @Scheduled(fixedDelayString = "${app.aws.sqs.polling-interval-ms:10000}")
    public void processMessages() {
        try {
            int processedMessages = documentQueueConsumer.processOneMessage();

            if (processedMessages > 0) {
                logger.info("Processed {} SQS document message(s)", processedMessages);
            }
        } catch (BusinessException exception) {
            logger.error("Failed to process SQS document message: {}", exception.getMessage());
        }
    }
}