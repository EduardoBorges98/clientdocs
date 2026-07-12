package com.eduardo.clientdocs.controller;

import com.eduardo.clientdocs.queue.DocumentQueueConsumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ConditionalOnProperty(name = "app.aws.sqs.document-queue-url")
public class QueueController {

    private final DocumentQueueConsumer documentQueueConsumer;

    public QueueController(DocumentQueueConsumer documentQueueConsumer) {
        this.documentQueueConsumer = documentQueueConsumer;
    }

    @PostMapping("/queue/process-one")
    public Map<String, Object> processOne() {
        int processedMessages = documentQueueConsumer.processOneMessage();

        return Map.of(
                "processedMessages", processedMessages
        );
    }
}