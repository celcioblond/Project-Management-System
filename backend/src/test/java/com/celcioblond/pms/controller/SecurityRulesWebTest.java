package com.celcioblond.pms.controller;

import com.celcioblond.pms.TestUsers;
import com.celcioblond.pms.dto.UserResponse;
import com.celcioblond.pms.security.AppUserDetailsService;
import com.celcioblond.pms.security.JwtService;
import com.celcioblond.pms.security.SecurityConfig;
import com.celcioblond.pms.service.ProjectService;
import com.celcioblond.pms.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the URL-level authorization rules in {@link SecurityConfig} and the
 * request validation, with the service layer mocked out.
 */
@WebMvcTest(controllers = {UserController.class, ProjectController.class, AuthController.class})
@Import(SecurityConfig.class)
@TestPropertySource("classpath:test.properties")
class SecurityRulesWebTest {

    private static final String VALID_USER = """
            {"name":"Dana","username":"dana01","email":"dana@example.com",
             "password":"Passw0rd!x","role":"ADMIN"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private ProjectService projectService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private AppUserDetailsService userDetailsService;

    @Test
    void anonymousRequestsAreRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/projects/my-projects")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void employeeCannotCreateUsers() throws Exception {
        mockMvc.perform(post("/api/users").with(user(TestUsers.employee(7L)))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_USER))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void employeeCannotChangeRolesOrListUsers() throws Exception {
        mockMvc.perform(put("/api/users/7").with(user(TestUsers.employee(7L)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users").with(user(TestUsers.employee(7L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCannotDeleteProjectsOrUseAiGeneration() throws Exception {
        mockMvc.perform(delete("/api/projects/1").with(user(TestUsers.employee(7L))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/project/generate-project").with(user(TestUsers.employee(7L)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\",\"type\":\"y\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(projectService);
    }

    @Test
    void employeeCanReadOwnProjects() throws Exception {
        when(projectService.getProjectsForUser(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/projects/my-projects").with(user(TestUsers.employee(7L))))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanCreateUsers() throws Exception {
        when(userService.createUser(any())).thenReturn(
                new UserResponse(2L, "Dana", 30, "dana@example.com", "dana01", null, null, "ADMIN"));

        mockMvc.perform(post("/api/users").with(user(TestUsers.admin()))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_USER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("dana01"));
    }

    @Test
    void invalidPayloadReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ab\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());

        verifyNoInteractions(userService);
    }
}
