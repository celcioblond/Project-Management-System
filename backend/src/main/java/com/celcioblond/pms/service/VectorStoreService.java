package com.celcioblond.pms.service;

import com.celcioblond.pms.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Central service for building, syncing and searching vector store documents.
 * All other services (Project, Task, User, Comment) call this instead
 * of duplicating document-building logic.
 *
 * Every project, task and comment document carries a {@value #SCOPE_KEY}
 * metadata entry so searches can be restricted to the projects a user may see.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    /** Bump when the document format or metadata changes; the seeder re-indexes older documents. */
    public static final String SCHEMA_VERSION = "2";
    public static final String SCHEMA_VERSION_KEY = "schemaVersion";
    static final String SCOPE_KEY = "scopeProjectId";

    private static final int TOP_K = 20;
    private static final double SIMILARITY_THRESHOLD = 0.3;

    private final VectorStore vectorStore;

    // ─── Public upsert methods (called by other services) ────────────────────

    public void upsertProject(Project project) {
        upsert("projectId", project.getId(), () -> buildProjectDocument(project));
    }

    public void upsertTask(Task task) {
        upsert("taskId", task.getId(), () -> buildTaskDocument(task));
    }

    public void upsertUser(User user) {
        upsert("userId", user.getId(), () -> buildUserDocument(user));
    }

    public void upsertProjectComment(ProjectComment comment) {
        upsert("projectCommentId", comment.getId(), () -> buildProjectCommentDocument(comment));
    }

    public void upsertTaskComment(TaskComment comment) {
        upsert("taskCommentId", comment.getId(), () -> buildTaskCommentDocument(comment));
    }

    // ─── Public delete methods ────────────────────────────────────────────────

    /** Removes the project document and every task/comment document scoped to it. */
    public void deleteProject(Long id)         { deleteByMeta(SCOPE_KEY,          String.valueOf(id)); }
    public void deleteTask(Long id)            { deleteByMeta("taskId",           String.valueOf(id)); }
    public void deleteUser(Long id)            { deleteByMeta("userId",           String.valueOf(id)); }
    public void deleteProjectComment(Long id)  { deleteByMeta("projectCommentId", String.valueOf(id)); }
    public void deleteTaskComment(Long id)     { deleteByMeta("taskCommentId",    String.valueOf(id)); }

    // ─── Search ───────────────────────────────────────────────────────────────

    /**
     * Semantic search over everything (admins) or, for employees, only over documents
     * of the given projects plus their own user profile.
     */
    public List<Document> search(String query, Long userId, boolean unrestricted, List<Long> projectIds) {
        SearchRequest.Builder request = SearchRequest.builder()
                .query(query)
                .topK(TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD);

        if (!unrestricted) {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            FilterExpressionBuilder.Op ownProfile = b.eq("userId", String.valueOf(userId));
            request.filterExpression(projectIds.isEmpty()
                    ? ownProfile.build()
                    : b.or(b.in(SCOPE_KEY, projectIds.stream().map(String::valueOf).toArray()), ownProfile).build());
        }
        return vectorStore.similaritySearch(request.build());
    }

    // ─── Document builders ────────────────────────────────────────────────────

    public Document buildProjectDocument(Project project) {
        String taskSummary = project.getTasks().isEmpty()
                ? "  No tasks assigned yet."
                : project.getTasks().stream()
                .map(t -> String.format("  - %s (Status: %s, Priority: %s, Due: %s, Assigned to: %s)",
                        t.getTitle(), t.getStatus(), t.getPriority(),
                        t.getDueDate() != null ? t.getDueDate().toLocalDate() : "No due date",
                        joinNames(t.getAssignedEmployees(), "Unassigned")))
                .collect(Collectors.joining("\n"));

        String content = String.format("""
                Project Name: %s
                Description: %s
                Type: %s
                Status: %s
                Start Date: %s
                End Date: %s
                Created At: %s
                Created By Admin: %s
                Assigned Employees: %s
                Tasks:
                %s
                """,
                project.getName(),
                project.getDescription(),
                project.getType(),
                project.getStatus(),
                project.getStartDate() != null ? project.getStartDate().toLocalDate() : "N/A",
                project.getEndDate() != null ? project.getEndDate().toLocalDate() : "N/A",
                project.getCreatedAt() != null ? project.getCreatedAt().toLocalDate() : "N/A",
                project.getCreatedByAdmin() != null ? project.getCreatedByAdmin().getName() : "Unknown",
                joinNames(project.getAssignedEmployees(), "None"),
                taskSummary);

        return new Document(UUID.randomUUID().toString(), content,
                metadata("projectId", project.getId(), "project", project.getId()));
    }

    public Document buildTaskDocument(Task task) {
        String comments = task.getComments().isEmpty()
                ? "  No comments."
                : task.getComments().stream()
                .map(c -> String.format("  - %s (by %s on %s)",
                        c.getContent(),
                        c.getAuthor() != null ? c.getAuthor().getName() : "Unknown",
                        c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate() : "N/A"))
                .collect(Collectors.joining("\n"));

        String content = String.format("""
                Task Title: %s
                Description: %s
                Status: %s
                Priority: %s
                Due Date: %s
                Project: %s
                Assigned To: %s
                Assigned By Admin: %s
                Created At: %s
                Comments:
                %s
                """,
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate() != null ? task.getDueDate().toLocalDate() : "N/A",
                task.getProject() != null ? task.getProject().getName() : "No project",
                joinNames(task.getAssignedEmployees(), "Unassigned"),
                task.getAssignedByAdmin() != null ? task.getAssignedByAdmin().getName() : "Unknown",
                task.getCreatedAt() != null ? task.getCreatedAt().toLocalDate() : "N/A",
                comments);

        return new Document(UUID.randomUUID().toString(), content,
                metadata("taskId", task.getId(), "task", projectIdOf(task)));
    }

    public Document buildUserDocument(User user) {
        String content = String.format("""
                Team Member Name: %s
                Username: %s
                Email: %s
                Role: %s
                Position: %s
                Department: %s
                Age: %s
                """,
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getPosition() != null ? user.getPosition() : "N/A",
                user.getDepartment() != null ? user.getDepartment() : "N/A",
                user.getAge());

        return new Document(UUID.randomUUID().toString(), content,
                metadata("userId", user.getId(), "user", null));
    }

    public Document buildProjectCommentDocument(ProjectComment comment) {
        String content = String.format("""
                Project Comment
                Content: %s
                Author: %s
                Project: %s
                Created At: %s
                """,
                comment.getContent(),
                comment.getAuthor() != null ? comment.getAuthor().getName() : "Unknown",
                comment.getProject() != null ? comment.getProject().getName() : "Unknown",
                comment.getCreatedAt() != null ? comment.getCreatedAt().toLocalDate() : "N/A");

        return new Document(UUID.randomUUID().toString(), content,
                metadata("projectCommentId", comment.getId(), "project_comment",
                        comment.getProject() != null ? comment.getProject().getId() : null));
    }

    public Document buildTaskCommentDocument(TaskComment comment) {
        String content = String.format("""
                Task Comment
                Content: %s
                Author: %s
                Task: %s
                Project: %s
                Created At: %s
                """,
                comment.getContent(),
                comment.getAuthor() != null ? comment.getAuthor().getName() : "Unknown",
                comment.getTask() != null ? comment.getTask().getTitle() : "Unknown",
                comment.getTask() != null && comment.getTask().getProject() != null
                        ? comment.getTask().getProject().getName() : "Unknown",
                comment.getCreatedAt() != null ? comment.getCreatedAt().toLocalDate() : "N/A");

        return new Document(UUID.randomUUID().toString(), content,
                metadata("taskCommentId", comment.getId(), "task_comment",
                        comment.getTask() != null ? projectIdOf(comment.getTask()) : null));
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private static Map<String, Object> metadata(String idKey, Long id, String type, Long scopeProjectId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(idKey, String.valueOf(id));
        metadata.put("type", type);
        metadata.put(SCHEMA_VERSION_KEY, SCHEMA_VERSION);
        if (scopeProjectId != null) {
            metadata.put(SCOPE_KEY, String.valueOf(scopeProjectId));
        }
        return metadata;
    }

    private static Long projectIdOf(Task task) {
        return task.getProject() != null ? task.getProject().getId() : null;
    }

    private static String joinNames(List<User> users, String fallback) {
        return users.isEmpty() ? fallback : users.stream().map(User::getName).collect(Collectors.joining(", "));
    }

    /**
     * Replaces the document for one entity. Embedding calls go to OpenAI, so a failure
     * here is logged instead of propagated: the chatbot context may lag behind, but
     * regular CRUD keeps working when the AI provider is unavailable.
     */
    private void upsert(String key, Long id, Supplier<Document> document) {
        try {
            deleteByMeta(key, String.valueOf(id));
            vectorStore.add(List.of(document.get()));
        } catch (Exception e) {
            log.warn("Could not index {}={} in the vector store: {}", key, id, e.getMessage());
        }
    }

    private void deleteByMeta(String key, String value) {
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            vectorStore.delete(b.eq(key, value).build());
        } catch (Exception e) {
            // Don't fail the main operation if the vector delete has an issue
            log.warn("Could not delete vector doc for {}={}: {}", key, value, e.getMessage());
        }
    }
}
