package de.jivz.mcp.tools.git;

import de.jivz.mcp.tools.Tool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration Tests для GitHub Issue Management Tools
 * Тестирует полную интеграцию с Spring Context
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("GitHub Issue Tools Integration Tests")
class GitHubIssueToolsIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private CreateGitHubIssueTool createGitHubIssueTool;

    @Autowired(required = false)
    private UpdateGitHubIssueTool updateGitHubIssueTool;

    @Autowired(required = false)
    private DeleteGitHubIssueTool deleteGitHubIssueTool;

    @Autowired(required = false)
    private ListGitHubIssuesTool listGitHubIssuesTool;

    // ============================================
    // Spring Context Tests
    // ============================================

    @Test
    @DisplayName("Spring Context should load successfully")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    @DisplayName("All GitHub Issue Tools should be loaded as Spring Beans")
    void allToolsShouldBeLoadedAsBeans() {
        assertThat(createGitHubIssueTool).isNotNull();
        assertThat(updateGitHubIssueTool).isNotNull();
        assertThat(deleteGitHubIssueTool).isNotNull();
        assertThat(listGitHubIssuesTool).isNotNull();
    }

    @Test
    @DisplayName("Should be able to get all Tool beans from context")
    void shouldGetAllToolBeansFromContext() {
        // When
        Map<String, Tool> tools = applicationContext.getBeansOfType(Tool.class);

        // Then
        assertThat(tools).isNotEmpty();

        List<String> toolNames = tools.values().stream()
                .map(Tool::getName)
                .toList();

        assertThat(toolNames)
                .contains(
                        "create_github_issue",
                        "update_github_issue",
                        "delete_github_issue",
                        "list_github_issues"
                );
    }

    // ============================================
    // Tool Configuration Tests
    // ============================================

    @Test
    @DisplayName("CreateGitHubIssueTool should have test configuration injected")
    void createToolShouldHaveTestConfig() {
        assertThat(createGitHubIssueTool).isNotNull();
        assertThat(createGitHubIssueTool.getName()).isEqualTo("create_github_issue");
        assertThat(createGitHubIssueTool.getDefinition()).isNotNull();
    }

    @Test
    @DisplayName("UpdateGitHubIssueTool should have test configuration injected")
    void updateToolShouldHaveTestConfig() {
        assertThat(updateGitHubIssueTool).isNotNull();
        assertThat(updateGitHubIssueTool.getName()).isEqualTo("update_github_issue");
        assertThat(updateGitHubIssueTool.getDefinition()).isNotNull();
    }

    @Test
    @DisplayName("DeleteGitHubIssueTool should have test configuration injected")
    void deleteToolShouldHaveTestConfig() {
        assertThat(deleteGitHubIssueTool).isNotNull();
        assertThat(deleteGitHubIssueTool.getName()).isEqualTo("delete_github_issue");
        assertThat(deleteGitHubIssueTool.getDefinition()).isNotNull();
    }

    @Test
    @DisplayName("ListGitHubIssuesTool should have test configuration injected")
    void listToolShouldHaveTestConfig() {
        assertThat(listGitHubIssuesTool).isNotNull();
        assertThat(listGitHubIssuesTool.getName()).isEqualTo("list_github_issues");
        assertThat(listGitHubIssuesTool.getDefinition()).isNotNull();
    }

    // ============================================
    // Tool Definition Tests
    // ============================================

    @Test
    @DisplayName("All tools should have unique names")
    void allToolsShouldHaveUniqueNames() {
        // Given
        List<String> toolNames = List.of(
                createGitHubIssueTool.getName(),
                updateGitHubIssueTool.getName(),
                deleteGitHubIssueTool.getName(),
                listGitHubIssuesTool.getName()
        );

        // When & Then
        assertThat(toolNames)
                .hasSize(4)
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("All tools should have non-null definitions")
    void allToolsShouldHaveDefinitions() {
        assertThat(createGitHubIssueTool.getDefinition()).isNotNull();
        assertThat(updateGitHubIssueTool.getDefinition()).isNotNull();
        assertThat(deleteGitHubIssueTool.getDefinition()).isNotNull();
        assertThat(listGitHubIssuesTool.getDefinition()).isNotNull();
    }

    @Test
    @DisplayName("All tool definitions should have descriptions")
    void allToolDefinitionsShouldHaveDescriptions() {
        assertThat(createGitHubIssueTool.getDefinition().getDescription())
                .isNotBlank()
                .contains("issue");

        assertThat(updateGitHubIssueTool.getDefinition().getDescription())
                .isNotBlank()
                .contains("issue");

        assertThat(deleteGitHubIssueTool.getDefinition().getDescription())
                .isNotBlank()
                .contains("issue");

        assertThat(listGitHubIssuesTool.getDefinition().getDescription())
                .isNotBlank()
                .contains("issue");
    }

    @Test
    @DisplayName("All tool definitions should have input schemas")
    void allToolDefinitionsShouldHaveInputSchemas() {
        assertThat(createGitHubIssueTool.getDefinition().getInputSchema()).isNotNull();
        assertThat(updateGitHubIssueTool.getDefinition().getInputSchema()).isNotNull();
        assertThat(deleteGitHubIssueTool.getDefinition().getInputSchema()).isNotNull();
        assertThat(listGitHubIssuesTool.getDefinition().getInputSchema()).isNotNull();
    }

    @Test
    @DisplayName("CreateGitHubIssueTool should require 'title' parameter")
    void createToolShouldRequireTitle() {
        var required = createGitHubIssueTool.getDefinition().getInputSchema().getRequired();
        assertThat(required).contains("title");
    }

    @Test
    @DisplayName("UpdateGitHubIssueTool should require 'issueNumber' parameter")
    void updateToolShouldRequireIssueNumber() {
        var required = updateGitHubIssueTool.getDefinition().getInputSchema().getRequired();
        assertThat(required).contains("issueNumber");
    }

    @Test
    @DisplayName("DeleteGitHubIssueTool should require 'issueNumber' parameter")
    void deleteToolShouldRequireIssueNumber() {
        var required = deleteGitHubIssueTool.getDefinition().getInputSchema().getRequired();
        assertThat(required).contains("issueNumber");
    }

    // ============================================
    // Tool Discovery Tests
    // ============================================

    @Test
    @DisplayName("Tools should be discoverable by their names")
    void toolsShouldBeDiscoverableByNames() {
        // Given
        Map<String, Tool> allTools = applicationContext.getBeansOfType(Tool.class);

        // When
        Tool createTool = allTools.values().stream()
                .filter(t -> "create_github_issue".equals(t.getName()))
                .findFirst()
                .orElse(null);

        Tool updateTool = allTools.values().stream()
                .filter(t -> "update_github_issue".equals(t.getName()))
                .findFirst()
                .orElse(null);

        Tool deleteTool = allTools.values().stream()
                .filter(t -> "delete_github_issue".equals(t.getName()))
                .findFirst()
                .orElse(null);

        Tool listTool = allTools.values().stream()
                .filter(t -> "list_github_issues".equals(t.getName()))
                .findFirst()
                .orElse(null);

        // Then
        assertThat(createTool).isNotNull().isInstanceOf(CreateGitHubIssueTool.class);
        assertThat(updateTool).isNotNull().isInstanceOf(UpdateGitHubIssueTool.class);
        assertThat(deleteTool).isNotNull().isInstanceOf(DeleteGitHubIssueTool.class);
        assertThat(listTool).isNotNull().isInstanceOf(ListGitHubIssuesTool.class);
    }

    @Test
    @DisplayName("Should have at least 10 tools registered (including Git tools)")
    void shouldHaveMultipleToolsRegistered() {
        // Given
        Map<String, Tool> allTools = applicationContext.getBeansOfType(Tool.class);

        // Then
        assertThat(allTools.size()).isGreaterThanOrEqualTo(10);
    }

    // ============================================
    // Integration with other Git Tools
    // ============================================

    @Test
    @DisplayName("GitHub Issue Tools should coexist with other Git tools")
    void issueToolsShouldCoexistWithOtherGitTools() {
        // Given
        Map<String, Tool> allTools = applicationContext.getBeansOfType(Tool.class);
        List<String> toolNames = allTools.values().stream()
                .map(Tool::getName)
                .toList();

        // Then - проверяем что есть и другие Git tools
        assertThat(toolNames)
                .contains("get_current_branch")  // Existing tool
                .contains("get_git_status")      // Existing tool
                .contains("create_github_issue") // New tool
                .contains("list_github_issues"); // New tool
    }
}

