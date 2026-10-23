package com.celcioblond.pms.service;

import com.celcioblond.pms.security.AccessPolicy;
import com.celcioblond.pms.security.UserPrincipal;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG chatbot. The retrieved context is restricted to documents the current
 * user is allowed to see, so access control never depends on the prompt.
 */
@Service
public class ChatBotService {

    private final VectorStoreService vectorStoreService;
    private final AccessPolicy accessPolicy;
    private final ChatClient chatClient;
    private final String promptTemplate;

    public ChatBotService(VectorStoreService vectorStoreService,
                          AccessPolicy accessPolicy,
                          ChatClient chatClient,
                          @Value("classpath:prompts/chatbot-rag-prompt.st") Resource promptResource) throws IOException {
        this.vectorStoreService = vectorStoreService;
        this.accessPolicy = accessPolicy;
        this.chatClient = chatClient;
        // Read via stream (not getFile()) so it also works when packaged inside a jar
        this.promptTemplate = promptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public String getBotResponse(String userQuery, UserPrincipal currentUser) {
        PromptTemplate template = PromptTemplate.builder()
                .template(promptTemplate)
                .variables(Map.of(
                        "userQuery", userQuery,
                        "context", fetchSemanticContext(userQuery, currentUser),
                        "username", currentUser.getUsername()))
                .build();

        return chatClient.prompt(template.create()).call().content();
    }

    private String fetchSemanticContext(String userQuery, UserPrincipal currentUser) {
        List<Long> projectIds = currentUser.isAdmin() ? List.of() : accessPolicy.accessibleProjectIds(currentUser);
        List<Document> documents = vectorStoreService.search(
                userQuery, currentUser.getId(), currentUser.isAdmin(), projectIds);

        return documents.stream()
                .map(document -> document.getFormattedContent() + "\n---\n")
                .collect(Collectors.joining());
    }
}
