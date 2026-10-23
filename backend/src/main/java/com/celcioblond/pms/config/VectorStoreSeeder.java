package com.celcioblond.pms.config;

import com.celcioblond.pms.model.*;
import com.celcioblond.pms.repository.*;
import com.celcioblond.pms.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

/**
 * Runs once on startup. For every entity that has no document yet in the
 * vector store (checked via the metadata projectId / taskId / userId),
 * it creates and adds a document so the RAG chatbot has full context.
 *
 * Safe to re-run — it checks existence first and skips already-indexed items.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStoreSeeder implements ApplicationRunner {

    private final VectorStore vectorStore;
    private final VectorStoreService vectorStoreService;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectCommentRepository projectCommentRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting vector store seeding...");

        seed("projects", "projectId", projectRepository.findAll(),
                Project::getId, vectorStoreService::buildProjectDocument);
        seed("tasks", "taskId", taskRepository.findAll(),
                Task::getId, vectorStoreService::buildTaskDocument);
        seed("users", "userId", userRepository.findAll(),
                User::getId, vectorStoreService::buildUserDocument);
        seed("project comments", "projectCommentId", projectCommentRepository.findAll(),
                ProjectComment::getId, vectorStoreService::buildProjectCommentDocument);
        seed("task comments", "taskCommentId", taskCommentRepository.findAll(),
                TaskComment::getId, vectorStoreService::buildTaskCommentDocument);

        log.info("Vector store seeding complete.");
    }

    private <T> void seed(String label, String metadataKey, List<T> entities,
                          Function<T, Long> idOf, Function<T, Document> toDocument) {
        List<Document> toAdd = entities.stream()
                .filter(e -> !existsInVectorStore(metadataKey, String.valueOf(idOf.apply(e))))
                .map(toDocument)
                .toList();

        if (toAdd.isEmpty()) {
            log.info("All {} already indexed.", label);
        } else {
            vectorStore.add(toAdd);
            log.info("Seeded {} {}.", toAdd.size(), label);
        }
    }

    /**
     * Checks the vector_store table directly to see if a document with the
     * given metadata key/value already exists, avoiding duplicate embeddings.
     */
    private boolean existsInVectorStore(String metadataKey, String metadataValue) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vector_store WHERE metadata->>? = ?",
                    Integer.class,
                    metadataKey, metadataValue);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
