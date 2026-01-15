package de.jivz.mcp.tools.git;

import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.ToolExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit Tests für GitHub Issue Management Tools
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GitHub Issue Tools Unit Tests")
class GitHubIssueToolsUnitTest {

    private final String testToken = "ghp_test_token_12345";
    private final String testRepository = "test-owner/test-repo";

    // ============================================
    // CreateGitHubIssueTool Tests
    // ============================================

    @Test
    @DisplayName("CreateGitHubIssueTool - Should have correct name")
    void createTool_shouldHaveCorrectName() {
        // Given
        CreateGitHubIssueTool tool = new CreateGitHubIssueTool();

        // When & Then
        assertThat(tool.getName()).isEqualTo("create_github_issue");
    }

    @Test
    @DisplayName("CreateGitHubIssueTool - Should have correct definition")
    void createTool_shouldHaveCorrectDefinition() {
        // Given
        CreateGitHubIssueTool tool = new CreateGitHubIssueTool();

        // When
        ToolDefinition definition = tool.getDefinition();

        // Then
        assertThat(definition).isNotNull();
        assertThat(definition.getName()).isEqualTo("create_github_issue");
        assertThat(definition.getDescription()).contains("Create a new issue");
        assertThat(definition.getInputSchema()).isNotNull();
        assertThat(definition.getInputSchema().getProperties()).containsKeys(
                "repository", "title", "body", "labels", "assignees", "milestone"
        );
        assertThat(definition.getInputSchema().getRequired()).contains("title");
    }

    @Test
    @DisplayName("CreateGitHubIssueTool - Should fail without title")
    void createTool_shouldFailWithoutTitle() {
        // Given
        CreateGitHubIssueTool tool = new CreateGitHubIssueTool();
        ReflectionTestUtils.setField(tool, "githubToken", testToken);
        ReflectionTestUtils.setField(tool, "defaultRepository", testRepository);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("body", "Test body");

        // When & Then
        assertThatThrownBy(() -> tool.execute(arguments))
                .isInstanceOf(ToolExecutionException.class)
                .hasMessageContaining("Title is required");
    }

    // ============================================
    // UpdateGitHubIssueTool Tests
    // ============================================

    @Test
    @DisplayName("UpdateGitHubIssueTool - Should have correct name")
    void updateTool_shouldHaveCorrectName() {
        // Given
        UpdateGitHubIssueTool tool = new UpdateGitHubIssueTool();

        // When & Then
        assertThat(tool.getName()).isEqualTo("update_github_issue");
    }

    @Test
    @DisplayName("UpdateGitHubIssueTool - Should have correct definition")
    void updateTool_shouldHaveCorrectDefinition() {
        // Given
        UpdateGitHubIssueTool tool = new UpdateGitHubIssueTool();

        // When
        ToolDefinition definition = tool.getDefinition();

        // Then
        assertThat(definition).isNotNull();
        assertThat(definition.getName()).isEqualTo("update_github_issue");
        assertThat(definition.getDescription()).contains("Update an existing");
        assertThat(definition.getInputSchema().getProperties()).containsKeys(
                "repository", "issueNumber", "title", "body", "state", "labels", "assignees", "milestone"
        );
        assertThat(definition.getInputSchema().getRequired()).contains("issueNumber");
    }

    @Test
    @DisplayName("UpdateGitHubIssueTool - Should fail without issue number")
    void updateTool_shouldFailWithoutIssueNumber() {
        // Given
        UpdateGitHubIssueTool tool = new UpdateGitHubIssueTool();
        ReflectionTestUtils.setField(tool, "githubToken", testToken);
        ReflectionTestUtils.setField(tool, "defaultRepository", testRepository);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("title", "Updated title");

        // When & Then
        assertThatThrownBy(() -> tool.execute(arguments))
                .isInstanceOf(ToolExecutionException.class)
                .hasMessageContaining("Issue number is required");
    }

    // ============================================
    // DeleteGitHubIssueTool Tests
    // ============================================

    @Test
    @DisplayName("DeleteGitHubIssueTool - Should have correct name")
    void deleteTool_shouldHaveCorrectName() {
        // Given
        DeleteGitHubIssueTool tool = new DeleteGitHubIssueTool();

        // When & Then
        assertThat(tool.getName()).isEqualTo("delete_github_issue");
    }

    @Test
    @DisplayName("DeleteGitHubIssueTool - Should have correct definition")
    void deleteTool_shouldHaveCorrectDefinition() {
        // Given
        DeleteGitHubIssueTool tool = new DeleteGitHubIssueTool();

        // When
        ToolDefinition definition = tool.getDefinition();

        // Then
        assertThat(definition).isNotNull();
        assertThat(definition.getName()).isEqualTo("delete_github_issue");
        assertThat(definition.getDescription()).contains("Close a GitHub issue");
        assertThat(definition.getInputSchema().getProperties()).containsKeys(
                "repository", "issueNumber", "reason", "comment"
        );
        assertThat(definition.getInputSchema().getRequired()).contains("issueNumber");
    }

    @Test
    @DisplayName("DeleteGitHubIssueTool - Should fail without issue number")
    void deleteTool_shouldFailWithoutIssueNumber() {
        // Given
        DeleteGitHubIssueTool tool = new DeleteGitHubIssueTool();
        ReflectionTestUtils.setField(tool, "githubToken", testToken);
        ReflectionTestUtils.setField(tool, "defaultRepository", testRepository);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("comment", "Closing this issue");

        // When & Then
        assertThatThrownBy(() -> tool.execute(arguments))
                .isInstanceOf(ToolExecutionException.class)
                .hasMessageContaining("Issue number is required");
    }

    // ============================================
    // ListGitHubIssuesTool Tests
    // ============================================

    @Test
    @DisplayName("ListGitHubIssuesTool - Should have correct name")
    void listTool_shouldHaveCorrectName() {
        // Given
        ListGitHubIssuesTool tool = new ListGitHubIssuesTool();

        // When & Then
        assertThat(tool.getName()).isEqualTo("list_github_issues");
    }

    @Test
    @DisplayName("ListGitHubIssuesTool - Should have correct definition")
    void listTool_shouldHaveCorrectDefinition() {
        // Given
        ListGitHubIssuesTool tool = new ListGitHubIssuesTool();

        // When
        ToolDefinition definition = tool.getDefinition();

        // Then
        assertThat(definition).isNotNull();
        assertThat(definition.getName()).isEqualTo("list_github_issues");
        assertThat(definition.getDescription()).contains("Get list of issues");
        assertThat(definition.getInputSchema().getProperties()).containsKeys(
                "repository", "state", "labels", "assignee", "creator", "limit"
        );
    }

    @Test
    @DisplayName("ListGitHubIssuesTool - Should fail without repository")
    void listTool_shouldFailWithoutRepository() {
        // Given
        ListGitHubIssuesTool tool = new ListGitHubIssuesTool();
        ReflectionTestUtils.setField(tool, "githubToken", null);
        ReflectionTestUtils.setField(tool, "defaultRepository", null);

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("state", "open");

        // When & Then
        assertThatThrownBy(() -> tool.execute(arguments))
                .isInstanceOf(ToolExecutionException.class)
                .hasMessageContaining("Repository parameter is required");
    }

    // ============================================
    // Parameter Validation Tests
    // ============================================

    @Test
    @DisplayName("Should validate labels parameter as array")
    void shouldValidateLabelsAsArray() {
        // Given
        CreateGitHubIssueTool tool = new CreateGitHubIssueTool();
        ToolDefinition definition = tool.getDefinition();

        // When
        var labelsProperty = definition.getInputSchema().getProperties().get("labels");

        // Then
        assertThat(labelsProperty).isNotNull();
        assertThat(labelsProperty.getType()).isEqualTo("array");
    }

    @Test
    @DisplayName("Should validate assignees parameter as array")
    void shouldValidateAssigneesAsArray() {
        // Given
        UpdateGitHubIssueTool tool = new UpdateGitHubIssueTool();
        ToolDefinition definition = tool.getDefinition();

        // When
        var assigneesProperty = definition.getInputSchema().getProperties().get("assignees");

        // Then
        assertThat(assigneesProperty).isNotNull();
        assertThat(assigneesProperty.getType()).isEqualTo("array");
    }

    @Test
    @DisplayName("All tools should be Spring Components")
    void allToolsShouldBeSpringComponents() {
        // When & Then
        assertThat(CreateGitHubIssueTool.class.isAnnotationPresent(org.springframework.stereotype.Component.class))
                .isTrue();
        assertThat(UpdateGitHubIssueTool.class.isAnnotationPresent(org.springframework.stereotype.Component.class))
                .isTrue();
        assertThat(DeleteGitHubIssueTool.class.isAnnotationPresent(org.springframework.stereotype.Component.class))
                .isTrue();
        assertThat(ListGitHubIssuesTool.class.isAnnotationPresent(org.springframework.stereotype.Component.class))
                .isTrue();
    }
}

