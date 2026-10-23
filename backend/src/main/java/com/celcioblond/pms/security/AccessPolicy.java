package com.celcioblond.pms.security;

import com.celcioblond.pms.model.Task;
import com.celcioblond.pms.model.User;
import com.celcioblond.pms.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Resource-level authorization rules. Admins can access everything; employees
 * only the projects they are assigned to (directly or through one of its tasks),
 * and only their own comments for edits.
 *
 * Must be called inside a transaction, since it walks lazy associations.
 */
@Component
@RequiredArgsConstructor
public class AccessPolicy {

    private final ProjectRepository projectRepository;

    public List<Long> accessibleProjectIds(UserPrincipal user) {
        return projectRepository.findAccessibleProjectIds(user.getId());
    }

    public void requireProjectAccess(Long projectId, UserPrincipal user) {
        if (!user.isAdmin() && !accessibleProjectIds(user).contains(projectId)) {
            throw new AccessDeniedException("No access to project " + projectId);
        }
    }

    public void requireTaskAccess(Task task, UserPrincipal user) {
        if (user.isAdmin() || isAssignedTo(task, user)) {
            return;
        }
        requireProjectAccess(task.getProject().getId(), user);
    }

    public void requireAuthorOrAdmin(User author, UserPrincipal user) {
        if (!user.isAdmin() && (author == null || !Objects.equals(author.getId(), user.getId()))) {
            throw new AccessDeniedException("Only the author can modify this comment");
        }
    }

    private static boolean isAssignedTo(Task task, UserPrincipal user) {
        return task.getAssignedEmployees().stream().anyMatch(u -> Objects.equals(u.getId(), user.getId()));
    }
}
