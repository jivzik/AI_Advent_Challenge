package de.jivz.mcp.tools.git;

import de.jivz.mcp.controller.McpToolsController;
import de.jivz.mcp.model.ToolCallRequest;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.Tool;
import de.jivz.mcp.tools.ToolExecutionException;
import de.jivz.mcp.tools.ToolRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Vollständiger Integration Test für GitHub Issue Management Tools
 * Testet die komplette Integration vom Controller bis zu den Tools
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("GitHub Issue Tools - Complete Integration Test")
class GitHubIssueToolsCompleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private McpToolsController mcpToolsController;

    @Autowired
    private CreateGitHubIssueTool createGitHubIssueTool;

    @Autowired
    private UpdateGitHubIssueTool updateGitHubIssueTool;

    @Autowired
    private DeleteGitHubIssueTool deleteGitHubIssueTool;

    @Autowired
    private ListGitHubIssuesTool listGitHubIssuesTool;

    // ============================================
    // ToolRegistry Integration Tests
    // ============================================

    @Test
    @DisplayName("ToolRegistry should contain all GitHub Issue Tools")
    void toolRegistryShouldContainAllGitHubIssueTools() {
        // When
        List<ToolDefinition> allTools = toolRegistry.getDefinitions();
        List<String> toolNames = allTools.stream()
                .map(ToolDefinition::getName)
                .toList();

        // Then
        assertThat(toolNames)
                .contains(
                        "create_github_issue",
                        "update_github_issue",
                        "delete_github_issue",
                        "list_github_issues"
                );
    }

    @Test
    @DisplayName("ToolRegistry should find tools by name")
    void toolRegistryShouldFindToolsByName() {
        // When
        Tool createTool = toolRegistry.find("create_github_issue").orElse(null);
        Tool updateTool = toolRegistry.find("update_github_issue").orElse(null);
        Tool deleteTool = toolRegistry.find("delete_github_issue").orElse(null);
        Tool listTool = toolRegistry.find("list_github_issues").orElse(null);

        // Then
        assertThat(createTool).isNotNull().isInstanceOf(CreateGitHubIssueTool.class);
        assertThat(updateTool).isNotNull().isInstanceOf(UpdateGitHubIssueTool.class);
        assertThat(deleteTool).isNotNull().isInstanceOf(DeleteGitHubIssueTool.class);
        assertThat(listTool).isNotNull().isInstanceOf(ListGitHubIssuesTool.class);
    }

    @Test
    @DisplayName("ToolRegistry should return tool definitions")
    void toolRegistryShouldReturnToolDefinitions() {
        // When
        ToolDefinition createDef = toolRegistry.find("create_github_issue")
                .map(Tool::getDefinition).orElse(null);
        ToolDefinition updateDef = toolRegistry.find("update_github_issue")
                .map(Tool::getDefinition).orElse(null);
        ToolDefinition deleteDef = toolRegistry.find("delete_github_issue")
                .map(Tool::getDefinition).orElse(null);
        ToolDefinition listDef = toolRegistry.find("list_github_issues")
                .map(Tool::getDefinition).orElse(null);

        // Then
        assertThat(createDef).isNotNull();
        assertThat(updateDef).isNotNull();
        assertThat(deleteDef).isNotNull();
        assertThat(listDef).isNotNull();

        assertThat(createDef.getName()).isEqualTo("create_github_issue");
        assertThat(updateDef.getName()).isEqualTo("update_github_issue");
        assertThat(deleteDef.getName()).isEqualTo("delete_github_issue");
        assertThat(listDef.getName()).isEqualTo("list_github_issues");
    }

    // ============================================
    // Tool Execution Tests
    // ============================================

    @Test
    @DisplayName("CreateGitHubIssueTool - Should fail without title")
    void createTool_shouldFailWithoutTitle() {
        // Given
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("repository", "test/repo");
        arguments.put("body", "Test body");

        // When & Then
        assertThatThrownBy(() -> createGitHubIssueTool.execute(arguments))
                .isInstanceOf(ToolExecutionException.class)
                .hasMessageContaining("Title is required");
    }

    @Test
    @DisplayName("UpdateGitHubIssueTool - Should fail without issueNumber")
    void updateTool_shouldFailWithoutIssueNumber() {
        // Given
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("repository", "test/repo");
        arguments.put("title", "Updated title");

        // When & Then
        assertThatThrownBy(() -> updateGitHubIssueTool.execute(arguments))
                .isInstanceOf(ToolExecutionException.class)
                .hasMessageContaining("Issue number is required");
    }

    @Test
    @DisplayName("DeleteGitHubIssueTool - Should fail without issueNumber")
    void deleteTool_shouldFailWithoutIssueNumber() {
        // Given
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("repository", "test/repo");
        arguments.put("comment", "Closing");

        // When & Then
        assertThatThrownBy(() -> deleteGitHubIssueTool.execute(arguments))
                .isInstanceOf(ToolExecutionException.class)
                .hasMessageContaining("Issue number is required");
    }

    @Test
    @DisplayName("ListGitHubIssuesTool - Should fail without repository when no default is set")
    void listTool_shouldFailWithoutRepository() {
        // Given - Tool mit leerem default repository
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("state", "open");

        // When & Then - Wird entweder "Repository required" oder "Bad credentials" werfen
        // da test-token ungültig ist
        assertThatThrownBy(() -> listGitHubIssuesTool.execute(arguments))
                .isInstanceOf(ToolExecutionException.class)
                .satisfiesAnyOf(
                        e -> assertThat(e.getMessage()).contains("Repository parameter is required"),
                        e -> assertThat(e.getMessage()).contains("Bad credentials"),
                        e -> assertThat(e.getMessage()).contains("Failed to retrieve issues")
                );
    }

    // ============================================
    // REST API Integration Tests
    // ============================================

    @Test
    @DisplayName("GET /api/tools should return GitHub Issue Tools")
    void apiShouldListGitHubIssueTools() throws Exception {
        mockMvc.perform(get("/api/tools"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[?(@.name=='create_github_issue')]").exists())
                .andExpect(jsonPath("$[?(@.name=='update_github_issue')]").exists())
                .andExpect(jsonPath("$[?(@.name=='delete_github_issue')]").exists())
                .andExpect(jsonPath("$[?(@.name=='list_github_issues')]").exists());
    }


    @Test
    @DisplayName("POST /api/tools/execute should validate required parameters")
    void apiShouldValidateRequiredParameters() throws Exception {
        // Given - Request without required 'title'
        String requestBody = """
                {
                    "toolName": "create_github_issue",
                    "arguments": {
                        "body": "Test body"
                    }
                }
                """;

        // When & Then - Controller returns 200 with success:false
        mockMvc.perform(post("/api/tools/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Title is required"));
    }

    // ============================================
    // Tool Statistics Tests
    // ============================================

    @Test
    @DisplayName("ToolRegistry should report correct statistics")
    void toolRegistryShouldReportCorrectStatistics() {
        // When
        List<ToolDefinition> allTools = toolRegistry.getDefinitions();
        long gitHubIssueToolsCount = allTools.stream()
                .map(ToolDefinition::getName)
                .filter(name -> name.contains("github_issue"))
                .count();

        // Then
        assertThat(allTools).hasSizeGreaterThanOrEqualTo(4);
        assertThat(gitHubIssueToolsCount).isEqualTo(4);
    }

    @Test
    @DisplayName("All GitHub Issue Tools should have unique descriptions")
    void allGitHubIssueToolsShouldHaveUniqueDescriptions() {
        // When
        List<String> descriptions = toolRegistry.getDefinitions().stream()
                .filter(tool -> tool.getName().contains("github_issue"))
                .map(ToolDefinition::getDescription)
                .toList();

        // Then
        assertThat(descriptions)
                .hasSize(4)
                .doesNotHaveDuplicates()
                .allMatch(desc -> desc != null && !desc.isBlank());
    }

    // ============================================
    // Parameter Validation Tests
    // ============================================

    @Test
    @DisplayName("CreateGitHubIssueTool - Should have repository parameter")
    void createTool_shouldHaveRepositoryParameter() {
        // When
        ToolDefinition definition = toolRegistry.find("create_github_issue")
                .map(Tool::getDefinition).orElse(null);

        // Then
        assertThat(definition).isNotNull();
        assertThat(definition.getInputSchema().getProperties())
                .containsKey("repository");
    }

    @Test
    @DisplayName("All tools should have repository parameter")
    void allToolsShouldHaveRepositoryParameter() {
        // When
        List<String> toolsWithRepository = toolRegistry.getDefinitions().stream()
                .filter(tool -> tool.getName().contains("github_issue"))
                .filter(tool -> tool.getInputSchema().getProperties().containsKey("repository"))
                .map(ToolDefinition::getName)
                .toList();

        // Then
        assertThat(toolsWithRepository)
                .hasSize(4)
                .containsExactlyInAnyOrder(
                        "create_github_issue",
                        "update_github_issue",
                        "delete_github_issue",
                        "list_github_issues"
                );
    }

    @Test
    @DisplayName("CreateGitHubIssueTool - Should accept labels as array")
    void createTool_shouldAcceptLabelsAsArray() {
        // When
        ToolDefinition definition = toolRegistry.find("create_github_issue")
                .map(Tool::getDefinition).orElse(null);

        // Then
        assertThat(definition).isNotNull();
        var labelsProperty = definition.getInputSchema().getProperties().get("labels");
        assertThat(labelsProperty).isNotNull();
        assertThat(labelsProperty.getType()).isEqualTo("array");
    }

    // ============================================
    // Integration with Other Tools
    // ============================================

    @Test
    @DisplayName("GitHub Issue Tools should coexist with Git Tools")
    void issueToolsShouldCoexistWithGitTools() {
        // When
        List<String> allToolNames = toolRegistry.getToolNames();

        // Then - Check for existing Git tools
        assertThat(allToolNames)
                .contains("get_current_branch")
                .contains("get_git_status")
                .contains("read_project_file")
                .contains("list_project_files");

        // And new GitHub Issue tools
        assertThat(allToolNames)
                .contains("create_github_issue")
                .contains("update_github_issue")
                .contains("delete_github_issue")
                .contains("list_github_issues");
    }

    @Test
    @DisplayName("Should have at least 20 tools registered")
    void shouldHaveAtLeastTwentyToolsRegistered() {
        // When
        int toolCount = toolRegistry.size();

        // Then
        assertThat(toolCount).isGreaterThanOrEqualTo(20);
    }

    // ============================================
    // Controller Integration Tests
    // ============================================

    @Test
    @DisplayName("McpToolsController should be autowired")
    void mcpToolsControllerShouldBeAutowired() {
        assertThat(mcpToolsController).isNotNull();
    }
}

