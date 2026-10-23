package com.celcioblond.pms.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests against a real PostgreSQL + pgvector database: Flyway migrations,
 * Hibernate schema validation, JWT authentication and resource-level authorization.
 * OpenAI is replaced by mocks so no network calls or API keys are needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource("classpath:test.properties")
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VectorStore vectorStore;
    @MockitoBean
    private ChatModel chatModel;

    private String adminToken;

    @BeforeEach
    void loginAsAdmin() throws Exception {
        adminToken = token(login("admin", "AdminPassw0rd!"));
    }

    @Test
    void employeeOnlySeesProjectsTheyBelongTo() throws Exception {
        Employee alice = register();
        Employee bob = register();
        long projectId = createProject(alice.id());

        perform(get("/api/projects/" + projectId), alice.token()).andExpect(status().isOk());
        perform(get("/api/projects/" + projectId), bob.token()).andExpect(status().isForbidden());
        perform(get("/api/projects/" + projectId + "/comments"), bob.token()).andExpect(status().isForbidden());

        JsonNode mine = json(perform(get("/api/projects/my-projects"), alice.token()).andExpect(status().isOk()));
        assertThat(mine).extracting(p -> p.get("id").asLong()).contains(projectId);
    }

    @Test
    void commentAuthorComesFromTheTokenAndOnlyTheAuthorCanDelete() throws Exception {
        Employee alice = register();
        Employee bob = register();
        long projectId = createProject(alice.id(), bob.id());

        // A forged authorId in the body is ignored
        JsonNode comment = json(perform(post("/api/project-comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Mine\",\"projectId\":" + projectId + ",\"authorId\":1}"),
                alice.token()).andExpect(status().isCreated()));
        assertThat(comment.get("authorName").asText()).isEqualTo(alice.username());

        long commentId = comment.get("id").asLong();
        perform(delete("/api/project-comments/" + commentId), bob.token()).andExpect(status().isForbidden());
        perform(delete("/api/project-comments/" + commentId), alice.token()).andExpect(status().isNoContent());
    }

    @Test
    void employeeCannotEscalateToAdmin() throws Exception {
        Employee alice = register();

        perform(put("/api/users/" + alice.id()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"), alice.token()).andExpect(status().isForbidden());

        JsonNode relogin = login(alice.username(), "Passw0rd!x");
        assertThat(relogin.get("role").asText()).isEqualTo("EMPLOYEE");
    }

    @Test
    void usersCreatedByAdminCanLogIn() throws Exception {
        String username = "user" + UUID.randomUUID().toString().substring(0, 8);
        perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Dana","username":"%s","email":"%s@example.com","password":"Passw0rd!x","role":"EMPLOYEE"}
                """.formatted(username, username)), adminToken).andExpect(status().isCreated());

        assertThat(login(username, "Passw0rd!x").get("token").asText()).isNotBlank();
    }

    @Test
    void chatbotSearchIsRestrictedForEmployees() throws Exception {
        Employee alice = register();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Hello!")))));

        perform(post("/api/chat/ask").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"What are my tasks?\"}"), alice.token()).andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().hasFilterExpression()).isTrue();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private record Employee(long id, String username, String token) {
    }

    private Employee register() throws Exception {
        String username = "emp" + UUID.randomUUID().toString().substring(0, 8);
        JsonNode response = json(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"email\":\"%s@example.com\",\"password\":\"Passw0rd!x\"}"
                                .formatted(username, username)))
                .andExpect(status().isCreated()));

        JsonNode users = json(perform(get("/api/users"), adminToken).andExpect(status().isOk()));
        long id = 0;
        for (JsonNode user : users) {
            if (user.get("username").asText().equals(username)) id = user.get("id").asLong();
        }
        return new Employee(id, username, token(response));
    }

    private long createProject(Long... employeeIds) throws Exception {
        String ids = String.join(",", java.util.Arrays.stream(employeeIds).map(String::valueOf).toList());
        JsonNode project = json(perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Bridge","description":"Steel bridge","type":"Civil","assignedEmployeeIds":[%s]}
                """.formatted(ids)), adminToken).andExpect(status().isCreated()));
        return project.get("id").asLong();
    }

    private JsonNode login(String username, String password) throws Exception {
        return json(mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk()));
    }

    private ResultActions perform(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                  String token) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + token));
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private static String token(JsonNode authResponse) {
        return authResponse.get("token").asText();
    }
}
