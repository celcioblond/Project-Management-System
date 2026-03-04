package com.example.Project_Management.config;

import com.example.Project_Management.model.*;
import com.example.Project_Management.repo.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Runs once on startup. For every entity that has no document yet in the
 * vector store (checked via the metadata projectId / taskId / userId),
 * it creates and adds a document so the RAG chatbot has full context.
 *
 * Safe to re-run — it checks existence first and skips already-indexed items.
 */
@Component
public class VectorStoreSeeder implements ApplicationRunner {

    @Autowired private VectorStore pgVectorStore;
    @Autowired private ProjectRepo projectRepo;
    @Autowired private TaskRepo taskRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private ProjectCommentRepo projectCommentRepo;
    @Autowired private TaskCommentRepo taskCommentRepo;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        System.out.println("[VectorStoreSeeder] Starting vector store seeding...");

        seedProjects();
        seedTasks();
        seedUsers();
        seedProjectComments();
        seedTaskComments();

        System.out.println("[VectorStoreSeeder] Seeding complete.");
    }

    // ─── Projects ────────────────────────────────────────────────────────────

    private void seedProjects() {
        List<Project> projects = projectRepo.findAll();
        List<Document> toAdd = new ArrayList<>();

        for (Project project : projects) {
            if (!existsInVectorStore("projectId", String.valueOf(project.getId()))) {
                toAdd.add(buildProjectDocument(project));
            }
        }

        if (!toAdd.isEmpty()) {
            pgVectorStore.add(toAdd);
            System.out.println("[VectorStoreSeeder] Seeded " + toAdd.size() + " projects.");
        } else {
            System.out.println("[VectorStoreSeeder] All projects already indexed.");
        }
    }

    private Document buildProjectDocument(Project project) {
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

    // ─── Tasks ───────────────────────────────────────────────────────────────

    private void seedTasks() {
        List<Task> tasks = taskRepo.findAll();
        List<Document> toAdd = new ArrayList<>();

        for (Task task : tasks) {
            if (!existsInVectorStore("taskId", String.valueOf(task.getId()))) {
                toAdd.add(buildTaskDocument(task));
            }
        }

        if (!toAdd.isEmpty()) {
            pgVectorStore.add(toAdd);
            System.out.println("[VectorStoreSeeder] Seeded " + toAdd.size() + " tasks.");
        } else {
            System.out.println("[VectorStoreSeeder] All tasks already indexed.");
        }
    }

    private Document buildTaskDocument(Task task) {
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

    // ─── Users ───────────────────────────────────────────────────────────────

    private void seedUsers() {
        List<User> users = userRepo.findAll();
        List<Document> toAdd = new ArrayList<>();

        for (User user : users) {
            if (!existsInVectorStore("userId", String.valueOf(user.getId()))) {
                toAdd.add(buildUserDocument(user));
            }
        }

        if (!toAdd.isEmpty()) {
            pgVectorStore.add(toAdd);
            System.out.println("[VectorStoreSeeder] Seeded " + toAdd.size() + " users.");
        } else {
            System.out.println("[VectorStoreSeeder] All users already indexed.");
        }
    }

    private Document buildUserDocument(User user) {
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

    // ─── Project Comments ────────────────────────────────────────────────────

    private void seedProjectComments() {
        List<ProjectComment> comments = projectCommentRepo.findAll();
        List<Document> toAdd = new ArrayList<>();

        for (ProjectComment comment : comments) {
            if (!existsInVectorStore("projectCommentId", String.valueOf(comment.getId()))) {
                toAdd.add(buildProjectCommentDocument(comment));
            }
        }

        if (!toAdd.isEmpty()) {
            pgVectorStore.add(toAdd);
            System.out.println("[VectorStoreSeeder] Seeded " + toAdd.size() + " project comments.");
        } else {
            System.out.println("[VectorStoreSeeder] All project comments already indexed.");
        }
    }

    private Document buildProjectCommentDocument(ProjectComment comment) {
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

    // ─── Task Comments ───────────────────────────────────────────────────────

    private void seedTaskComments() {
        List<TaskComment> comments = taskCommentRepo.findAll();
        List<Document> toAdd = new ArrayList<>();

        for (TaskComment comment : comments) {
            if (!existsInVectorStore("taskCommentId", String.valueOf(comment.getId()))) {
                toAdd.add(buildTaskCommentDocument(comment));
            }
        }

        if (!toAdd.isEmpty()) {
            pgVectorStore.add(toAdd);
            System.out.println("[VectorStoreSeeder] Seeded " + toAdd.size() + " task comments.");
        } else {
            System.out.println("[VectorStoreSeeder] All task comments already indexed.");
        }
    }

    private Document buildTaskCommentDocument(TaskComment comment) {
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

    // ─── Helper ──────────────────────────────────────────────────────────────

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