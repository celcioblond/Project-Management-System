package com.example.Project_Management.service;

import com.example.Project_Management.model.Project;
import com.example.Project_Management.model.ProjectComment;
import com.example.Project_Management.model.User;
import com.example.Project_Management.model.dto.ProjectCommentCreate;
import com.example.Project_Management.model.dto.ProjectCommentResponse;
import com.example.Project_Management.model.dto.ProjectCommentUpdate;
import com.example.Project_Management.repo.ProjectCommentRepo;
import com.example.Project_Management.repo.ProjectRepo;
import com.example.Project_Management.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectCommentService {

    @Autowired private ProjectCommentRepo projectCommentRepo;
    @Autowired private ProjectRepo projectRepo;
    @Autowired private UserRepo userRepo;
    @Autowired private VectorStoreService vectorStoreService;

    public List<ProjectCommentResponse> getAllProjectComments(Long projectId) {
        return projectCommentRepo.findByProjectId(projectId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ProjectCommentResponse getCommentById(Long id) {
        ProjectComment projectComment = projectCommentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
        return convertToResponse(projectComment);
    }

    @Transactional
    public ProjectCommentResponse createComment(ProjectCommentCreate projectCommentCreate) {
        Project project = projectRepo.findById(projectCommentCreate.projectId())
                .orElseThrow(() -> new RuntimeException("Project not found by id: " + projectCommentCreate.projectId()));

        User author = userRepo.findById(projectCommentCreate.authorId())
                .orElseThrow(() -> new RuntimeException("Author not found by id: " + projectCommentCreate.authorId()));

        ProjectComment comment = new ProjectComment();
        comment.setContent(projectCommentCreate.content());
        comment.setProject(project);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now());

        ProjectComment savedComment = projectCommentRepo.save(comment);

        // Index the new comment
        vectorStoreService.upsertProjectComment(savedComment);

        // Re-index the parent project so its comment context is fresh
        vectorStoreService.upsertProject(projectRepo.findById(project.getId()).orElse(project));

        return convertToResponse(savedComment);
    }

    @Transactional
    public ProjectCommentResponse updateComment(Long id, ProjectCommentUpdate projectCommentUpdate) {
        ProjectComment projectComment = projectCommentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
        projectComment.setContent(projectCommentUpdate.content());
        projectComment.setUpdatedAt(LocalDateTime.now());

        ProjectComment updatedComment = projectCommentRepo.save(projectComment);

        // Re-index the updated comment
        vectorStoreService.upsertProjectComment(updatedComment);

        return convertToResponse(updatedComment);
    }

    @Transactional
    public void deleteComment(Long id) {
        ProjectComment comment = projectCommentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));

        // Remove from vector store before deleting from DB
        vectorStoreService.deleteProjectComment(id);

        projectCommentRepo.delete(comment);
    }

    @Transactional
    public void deleteCommentsByProjectId(Long projectId) {
        List<ProjectComment> comments = projectCommentRepo.findByProjectId(projectId);

        // Remove each comment from vector store
        comments.forEach(c -> vectorStoreService.deleteProjectComment(c.getId()));

        projectCommentRepo.deleteAll(comments);
    }

    public List<ProjectCommentResponse> getCommentsByUsername(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return projectCommentRepo.findByAuthorId(user.getId()).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private ProjectCommentResponse convertToResponse(ProjectComment comment) {
        return new ProjectCommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getAuthor() != null ? comment.getAuthor().getName() : null,
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}