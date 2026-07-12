package com.eduardo.clientdocs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.aws")
public class AwsProperties {

    private Sqs sqs = new Sqs();

    public Sqs getSqs() {
        return sqs;
    }

    public void setSqs(Sqs sqs) {
        this.sqs = sqs;
    }

    public static class Sqs {

        private String documentQueueUrl;

        public String getDocumentQueueUrl() {
            return documentQueueUrl;
        }

        public void setDocumentQueueUrl(String documentQueueUrl) {
            this.documentQueueUrl = documentQueueUrl;
        }
    }
}