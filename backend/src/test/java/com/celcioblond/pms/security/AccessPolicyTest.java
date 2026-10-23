package com.celcioblond.pms.security;

import com.celcioblond.pms.TestUsers;
import com.celcioblond.pms.model.Project;
import com.celcioblond.pms.model.Role;
import com.celcioblond.pms.model.Task;
import com.celcioblond.pms.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessPolicyTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private AccessPolicy accessPolicy;

    @Test
    void adminCanAccessAnyProjectWithoutLookup() {
        assertThatCode(() -> accessPolicy.requireProjectAccess(42L, TestUsers.admin())).doesNotThrowAnyException();
        verifyNoInteractions(projectRepository);
    }

    @Test
    void employeeCanAccessAssignedProject() {
        when(projectRepository.findAccessibleProjectIds(7L)).thenReturn(List.of(42L));

        assertThatCode(() -> accessPolicy.requireProjectAccess(42L, TestUsers.employee(7L))).doesNotThrowAnyException();
    }

    @Test
    void employeeCannotAccessOtherProjects() {
        when(projectRepository.findAccessibleProjectIds(7L)).thenReturn(List.of(1L));

        assertThatThrownBy(() -> accessPolicy.requireProjectAccess(42L, TestUsers.employee(7L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assigneeCanAccessTaskEvenOutsideProjectAssignment() {
        Task task = taskInProject(42L);
        task.getAssignedEmployees().add(TestUsers.user(7L, "employee7", Role.EMPLOYEE));

        assertThatCode(() -> accessPolicy.requireTaskAccess(task, TestUsers.employee(7L))).doesNotThrowAnyException();
        verifyNoInteractions(projectRepository);
    }

    @Test
    void nonAssigneeOutsideProjectCannotAccessTask() {
        when(projectRepository.findAccessibleProjectIds(7L)).thenReturn(List.of());

        assertThatThrownBy(() -> accessPolicy.requireTaskAccess(taskInProject(42L), TestUsers.employee(7L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void onlyAuthorOrAdminCanModifyComment() {
        var author = TestUsers.user(7L, "employee7", Role.EMPLOYEE);

        assertThatCode(() -> accessPolicy.requireAuthorOrAdmin(author, TestUsers.employee(7L))).doesNotThrowAnyException();
        assertThatCode(() -> accessPolicy.requireAuthorOrAdmin(author, TestUsers.admin())).doesNotThrowAnyException();
        assertThatThrownBy(() -> accessPolicy.requireAuthorOrAdmin(author, TestUsers.employee(8L)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private static Task taskInProject(long projectId) {
        Project project = new Project();
        project.setId(projectId);
        Task task = new Task();
        task.setProject(project);
        return task;
    }
}
