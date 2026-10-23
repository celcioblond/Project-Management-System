package com.celcioblond.pms.service;

import com.celcioblond.pms.dto.ProjectCommentCreate;
import com.celcioblond.pms.dto.ProjectCommentResponse;
import com.celcioblond.pms.dto.ProjectCommentUpdate;
import com.celcioblond.pms.exception.ResourceNotFoundException;
import com.celcioblond.pms.mapper.ResponseMapper;
import com.celcioblond.pms.model.Project;
import com.celcioblond.pms.model.ProjectComment;
import com.celcioblond.pms.model.User;
import com.celcioblond.pms.repository.ProjectCommentRepository;
import com.celcioblond.pms.repository.ProjectRepository;
import com.celcioblond.pms.repository.UserRepository;
import com.celcioblond.pms.security.AccessPolicy;
import com.celcioblond.pms.security.UserPrincipal;
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
    private final AccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public List<ProjectCommentResponse> getAllProjectComments(Long projectId, UserPrincipal currentUser) {
        accessPolicy.requireProjectAccess(projectId, currentUser);
        return toResponses(projectCommentRepository.findByProjectId(projectId));
    }

    @Transactional(readOnly = true)
    public List<ProjectCommentResponse> getCommentsForUser(UserPrincipal currentUser) {
        return toResponses(projectCommentRepository.findByAuthorId(currentUser.getId()));
    }

    @Transactional
    public ProjectCommentResponse createComment(ProjectCommentCreate projectCommentCreate, UserPrincipal currentUser) {
        Long projectId = projectCommentCreate.projectId();
        accessPolicy.requireProjectAccess(projectId, currentUser);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        ProjectComment comment = new ProjectComment();
        comment.setContent(projectCommentCreate.content());
        comment.setProject(project);
        comment.setAuthor(author);
        comment.setCreatedAt(LocalDateTime.now());

        ProjectComment savedComment = projectCommentRepository.save(comment);
        project.getComments().add(savedComment);

        vectorStoreService.upsertProjectComment(savedComment);
        // Re-index the parent project so its comment context is fresh
        vectorStoreService.upsertProject(project);

        return ResponseMapper.toProjectCommentResponse(savedComment);
    }

    @Transactional
    public ProjectCommentResponse updateComment(Long id, ProjectCommentUpdate projectCommentUpdate,
                                                UserPrincipal currentUser) {
        ProjectComment comment = findComment(id);
        accessPolicy.requireAuthorOrAdmin(comment.getAuthor(), currentUser);

        comment.setContent(projectCommentUpdate.content());
        comment.setUpdatedAt(LocalDateTime.now());

        ProjectComment updatedComment = projectCommentRepository.save(comment);
        vectorStoreService.upsertProjectComment(updatedComment);

        return ResponseMapper.toProjectCommentResponse(updatedComment);
    }

    @Transactional
    public void deleteComment(Long id, UserPrincipal currentUser) {
        ProjectComment comment = findComment(id);
        accessPolicy.requireAuthorOrAdmin(comment.getAuthor(), currentUser);

        vectorStoreService.deleteProjectComment(id);
        comment.getProject().getComments().remove(comment);
        projectCommentRepository.delete(comment);
    }

    private ProjectComment findComment(Long id) {
        return projectCommentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Comment", id));
    }

    private static List<ProjectCommentResponse> toResponses(List<ProjectComment> comments) {
        return comments.stream().map(ResponseMapper::toProjectCommentResponse).toList();
    }
}
