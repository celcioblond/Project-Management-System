package com.example.Project_Management.service;

import com.example.Project_Management.model.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Central service for building and syncing vector store documents.
 * All other services (Project, Task, User, Comment) call this instead
 * of duplicating document-building logic.
 */
@Service
public class VectorStoreService {

    @Autowired private VectorStore pgVectorStore;
    @Autowired private JdbcTemplate jdbcTemplate;

    // ─── Public upsert methods (called by other services) ────────────────────

    public void upsertProject(Project project) {
        deleteByMeta("projectId", String.valueOf(project.getId()));
        pgVectorStore.add(List.of(buildProjectDocument(project)));
    }

    public void upsertTask(Task task) {
        deleteByMeta("taskId", String.valueOf(task.getId()));
        pgVectorStore.add(List.of(buildTaskDocument(task)));
    }

    public void upsertUser(User user) {
        deleteByMeta("userId", String.valueOf(user.getId()));
        pgVectorStore.add(List.of(buildUserDocument(user)));
    }

    public void upsertProjectComment(ProjectComment comment) {
        deleteByMeta("projectCommentId", String.valueOf(comment.getId()));
        pgVectorStore.add(List.of(buildProjectCommentDocument(comment)));
    }

    public void upsertTaskComment(TaskComment comment) {
        deleteByMeta("taskCommentId", String.valueOf(comment.getId()));
        pgVectorStore.add(List.of(buildTaskCommentDocument(comment)));
    }

    // ─── Public delete methods ────────────────────────────────────────────────

    public void deleteProject(Long id)         { deleteByMeta("projectId",        String.valueOf(id)); }
    public void deleteTask(Long id)            { deleteByMeta("taskId",           String.valueOf(id)); }
    public void deleteUser(Long id)            { deleteByMeta("userId",           String.valueOf(id)); }
    public void deleteProjectComment(Long id)  { deleteByMeta("projectCommentId", String.valueOf(id)); }
    public void deleteTaskComment(Long id)     { deleteByMeta("taskCommentId",    String.valueOf(id)); }

    // ─── Document builders ────────────────────────────────────────────────────

    public Document buildProjectDocument(Project project) {
        String assignedEmployees = project.getAssignedEmployees() != null
                ? project.getAssignedEmployees().stream().map(User::getName).collect(Collectors.joining(", "))
                : "None";

        String taskSummary = project.getTasks() != null && !project.getTasks().isEmpty()
                ? project.getTasks().stream()
                .map(t -> String.format("  - %s (Status: %s, Priority: %s, Due: %s, Assigned to: %s)",
                        t.getTitle(), t.getStatus(), t.getPriority(),
                        t.getDueDate() != null ? t.getDueDate().toLocalDate() : "No due date",
                        t.getAssignedEmployees() != null
                                ? t.getAssignedEmployees().stream().map(User::getName).collect(Collectors.joining(", "))
                                : "Unassigned"))
                .collect(Collectors.joining("\n"))
                : "  No tasks assigned yet.";

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
                assignedEmployees,
                taskSummary);

        return new Document(UUID.randomUUID().toString(), content,
                Map.of("projectId", String.valueOf(project.getId()), "type", "project"));
    }

    public Document buildTaskDocument(Task task) {
        String assignedEmployees = task.getAssignedEmployees() != null
                ? task.getAssignedEmployees().stream().map(User::getName).collect(Collectors.joining(", "))
                : "Unassigned";

        String comments = task.getComments() != null && !task.getComments().isEmpty()
                ? task.getComments().stream()
                .map(c -> String.format("  - %s (by %s on %s)",
                        c.getContent(),
                        c.getAuthor() != null ? c.getAuthor().getName() : "Unknown",
                        c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate() : "N/A"))
                .collect(Collectors.joining("\n"))
                : "  No comments.";

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
                assignedEmployees,
                task.getAssignedByAdmin() != null ? task.getAssignedByAdmin().getName() : "Unknown",
                task.getCreatedAt() != null ? task.getCreatedAt().toLocalDate() : "N/A",
                comments);

        return new Document(UUID.randomUUID().toString(), content,
                Map.of("taskId", String.valueOf(task.getId()), "type", "task"));
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
                Map.of("userId", String.valueOf(user.getId()), "type", "user"));
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
                Map.of("projectCommentId", String.valueOf(comment.getId()), "type", "project_comment"));
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
                Map.of("taskCommentId", String.valueOf(comment.getId()), "type", "task_comment"));
    }

    // ─── Internal helper ──────────────────────────────────────────────────────

    private void deleteByMeta(String key, String value) {
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            pgVectorStore.delete(b.eq(key, value).build());
        } catch (Exception e) {
            // Log but don't fail the main operation if vector delete has an issue
            System.err.println("[VectorStoreService] Warning: could not delete vector doc for " + key + "=" + value + ": " + e.getMessage());
        }
    }
}