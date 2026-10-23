package com.celcioblond.pms.mapper;

import com.celcioblond.pms.dto.*;
import com.celcioblond.pms.model.*;

import java.util.List;

/**
 * Single place where entities are converted to API response DTOs.
 */
public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getAge(),
                user.getEmail(),
                user.getUsername(),
                user.getPosition(),
                user.getDepartment(),
                user.getRole()
        );
    }

    public static ProjectResponse toProjectResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getType(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                names(project.getAssignedEmployees()),
                nameOf(project.getCreatedByAdmin()),
                project.getTasks().stream().map(ResponseMapper::toTaskResponse).toList(),
                project.getComments().stream().map(ResponseMapper::toProjectCommentResponse).toList(),
                project.getCreatedAt(),
                project.getProjectDiagram()
        );
    }

    public static TaskResponse toTaskResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getProject() != null ? task.getProject().getName() : null,
                names(task.getAssignedEmployees()),
                nameOf(task.getAssignedByAdmin()),
                task.getComments().stream().map(ResponseMapper::toTaskCommentResponse).toList(),
                task.getCreatedAt()
        );
    }

    public static ProjectCommentResponse toProjectCommentResponse(ProjectComment comment) {
        return new ProjectCommentResponse(
                comment.getId(),
                comment.getContent(),
                nameOf(comment.getAuthor()),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    public static TaskCommentResponse toTaskCommentResponse(TaskComment comment) {
        return new TaskCommentResponse(
                comment.getId(),
                comment.getContent(),
                nameOf(comment.getAuthor()),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private static List<String> names(List<User> users) {
        return users == null ? List.of() : users.stream().map(User::getName).toList();
    }

    private static String nameOf(User user) {
        return user != null ? user.getName() : null;
    }
}
