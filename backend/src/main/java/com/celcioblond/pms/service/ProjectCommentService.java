package com.celcioblond.pms.service;

import com.celcioblond.pms.dto.ProjectCommentCreate;
import com.celcioblond.pms.dto.ProjectCommentResponse;
import com.celcioblond.pms.dto.ProjectCommentUpdate;
import com.celcioblond.pms.mapper.ResponseMapper;
import com.celcioblond.pms.model.Project;
import com.celcioblond.pms.model.ProjectComment;
import com.celcioblond.pms.model.User;
import com.celcioblond.pms.repository.ProjectCommentRepository;
import com.celcioblond.pms.repository.ProjectRepository;
import com.celcioblond.pms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectCommentService {

    private final ProjectCommentRepository projectCommentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final VectorStoreService vectorStoreService;

    @Transactional(readOnly = true)
    public List<ProjectCommentResponse> getAllProjectComments(Long projectId) {
        return toResponses(projectCommentRepository.findByProjectId(projectId));
    }

    @Transactional(readOnly = true)
    public List<ProjectCommentResponse> getCommentsByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return toResponses(projectCommentRepository.findByAuthorId(user.getId()));
    }

    @Transactional
    public ProjectCommentResponse createComment(ProjectCommentCreate projectCommentCreate) {
        Project project = projectRepository.findById(projectCommentCreate.projectId())
                .orElseThrow(() -> new RuntimeException("Project not found by id: " + projectCommentCreate.projectId()));

        User author = userRepository.findById(projectCommentCreate.authorId())
                .orElseThrow(() -> new RuntimeException("Author not found by id: " + projectCommentCreate.authorId()));

        ProjectComment comment = new ProjectComment();
        comment.setContent(projectCommentCreate.content());
        comment.setProject(project);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now());

        ProjectComment savedComment = projectCommentRepository.save(comment);

        vectorStoreService.upsertProjectComment(savedComment);
        // Re-index the parent project so its comment context is fresh
        vectorStoreService.upsertProject(projectRepository.findById(project.getId()).orElse(project));

        return ResponseMapper.toProjectCommentResponse(savedComment);
    }

    @Transactional
    public ProjectCommentResponse updateComment(Long id, ProjectCommentUpdate projectCommentUpdate) {
        ProjectComment comment = findComment(id);
        comment.setContent(projectCommentUpdate.content());
        comment.setUpdatedAt(LocalDateTime.now());

        ProjectComment updatedComment = projectCommentRepository.save(comment);
        vectorStoreService.upsertProjectComment(updatedComment);

        return ResponseMapper.toProjectCommentResponse(updatedComment);
    }

    @Transactional
    public void deleteComment(Long id) {
        ProjectComment comment = findComment(id);
        vectorStoreService.deleteProjectComment(id);
        projectCommentRepository.delete(comment);
    }

    private ProjectComment findComment(Long id) {
        return projectCommentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
    }

    private static List<ProjectCommentResponse> toResponses(List<ProjectComment> comments) {
        return comments.stream().map(ResponseMapper::toProjectCommentResponse).toList();
    }
}
