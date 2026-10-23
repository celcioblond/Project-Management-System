package com.celcioblond.pms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatBotService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final String promptTemplate;

    public ChatBotService(VectorStore vectorStore,
                          ChatClient chatClient,
                          @Value("classpath:prompts/chatbot-rag-prompt.st") Resource promptResource) throws IOException {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        // Read via stream (not getFile()) so it also works when packaged inside a jar
        this.promptTemplate = promptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public String getBotResponse(String userQuery, String username) {
        try {
            PromptTemplate template = PromptTemplate.builder()
                    .template(promptTemplate)
                    .variables(Map.of(
                            "userQuery", userQuery,
                            "context", fetchSemanticContext(userQuery),
                            "username", username))
                    .build();

            return chatClient.prompt(template.create()).call().content();
        } catch (Exception e) {
            log.error("Chatbot request failed", e);
            return "Bot Failed: " + e.getMessage();
        }
    }

    private String fetchSemanticContext(String userQuery) {
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userQuery)
                        .topK(20)
                        .similarityThreshold(0.3)
                        .build()
        );

        return documents.stream()
                .map(document -> document.getFormattedContent() + "\n---\n")
                .collect(Collectors.joining());
    }
}
