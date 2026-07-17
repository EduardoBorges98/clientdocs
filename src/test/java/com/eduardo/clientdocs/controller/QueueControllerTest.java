package com.eduardo.clientdocs.controller;

import com.eduardo.clientdocs.queue.DocumentQueueConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class QueueControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DocumentQueueConsumer documentQueueConsumer;

    @BeforeEach
    void setUp() {
        QueueController queueController = new QueueController(documentQueueConsumer);

        mockMvc = MockMvcBuilders
                .standaloneSetup(queueController)
                .build();
    }

    @Test
    void shouldProcessOneMessageSuccessfully() throws Exception {
        when(documentQueueConsumer.processOneMessage())
                .thenReturn(1);

        mockMvc.perform(post("/queue/process-one"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedMessages").value(1));
    }

    @Test
    void shouldReturnZeroWhenNoMessageIsAvailable() throws Exception {
        when(documentQueueConsumer.processOneMessage())
                .thenReturn(0);

        mockMvc.perform(post("/queue/process-one"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedMessages").value(0));
    }
}