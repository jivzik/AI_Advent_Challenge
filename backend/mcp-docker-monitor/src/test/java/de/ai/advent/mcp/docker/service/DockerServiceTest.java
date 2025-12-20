package de.ai.advent.mcp.docker.service;

import de.ai.advent.mcp.docker.exception.SshConnectionException;
import de.ai.advent.mcp.docker.model.ContainerInfo;
import de.ai.advent.mcp.docker.ssh.SshManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DockerService
 *
 * Tests the 4 main methods:
 * - listContainers()
 * - getContainerLogs()
 * - checkContainerHealth()
 * - summarizeAll()
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DockerService Tests")
class DockerServiceTest {

    @Mock
    private SshManager sshManager;

    private DockerService dockerService;

    @BeforeEach
    void setUp() {
        dockerService = new DockerService(sshManager);
    }

    @Test
    @DisplayName("Should list containers successfully")
    void testListContainers() throws SshConnectionException, IOException {
        // Mock SSH response - docker ps output
        String mockResponse = "{\"ID\":\"abc123def456\",\"Names\":\"web-app\",\"Image\":\"myapp:latest\",\"State\":\"running\",\"Status\":\"Up 2 hours\",\"Ports\":\"0.0.0.0:8080->8080/tcp\"}\n" +
                             "{\"ID\":\"def456ghi789\",\"Names\":\"db-app\",\"Image\":\"postgres:latest\",\"State\":\"running\",\"Status\":\"Up 3 hours\",\"Ports\":\"5432/tcp\"}\n";

        when(sshManager.executeCommand("docker ps -a --format '{{json .}}'"))
            .thenReturn(mockResponse);

        // Execute
        List<ContainerInfo> containers = dockerService.listContainers();

        // Assert
        assertNotNull(containers);
        assertEquals(2, containers.size());
        assertEquals("web-app", containers.get(0).getName());
        assertEquals("db-app", containers.get(1).getName());
    }

    @Test
    @DisplayName("Should return empty list when no containers")
    void testListContainersEmpty() throws SshConnectionException, IOException {
        when(sshManager.executeCommand("docker ps -a --format '{{json .}}'"))
            .thenReturn("");

        List<ContainerInfo> containers = dockerService.listContainers();

        assertNotNull(containers);
        assertEquals(0, containers.size());
    }

    @Test
    @DisplayName("Should throw exception on SSH failure for listContainers")
    void testListContainersSSHError() throws SshConnectionException, IOException {
        when(sshManager.executeCommand("docker ps -a --format '{{json .}}'"))
            .thenThrow(new SshConnectionException("SSH connection failed"));

        assertThrows(RuntimeException.class, () -> dockerService.listContainers());
    }

    @Test
    @DisplayName("Should get container logs successfully")
    void testGetContainerLogs() throws SshConnectionException, IOException {
        String mockLogs = "2024-01-19 10:00:00 INFO: Application started\n" +
                         "2024-01-19 10:01:00 INFO: Request processed\n";

        when(sshManager.executeCommand("docker logs --tail 100 myapp"))
            .thenReturn(mockLogs);

        // Execute
        String logs = dockerService.getContainerLogs("myapp", 100, null);

        // Assert
        assertNotNull(logs);
        assertTrue(logs.contains("Application started"));
    }

    @Test
    @DisplayName("Should get container logs with since filter")
    void testGetContainerLogsWithSince() throws SshConnectionException, IOException {
        String mockLogs = "2024-01-19 10:50:00 INFO: Some event\n";

        when(sshManager.executeCommand("docker logs --tail 50 --since 1h myapp"))
            .thenReturn(mockLogs);

        // Execute
        String logs = dockerService.getContainerLogs("myapp", 50, "1h");

        // Assert
        assertNotNull(logs);
        assertEquals("2024-01-19 10:50:00 INFO: Some event\n", logs);
    }

    @Test
    @DisplayName("Should throw exception when container not found in logs")
    void testGetContainerLogsNotFound() throws SshConnectionException, IOException {
        when(sshManager.executeCommand(anyString()))
            .thenThrow(new IOException("No such container"));

        assertThrows(RuntimeException.class, () ->
            dockerService.getContainerLogs("nonexistent", 100, null));
    }

    @Test
    @DisplayName("Should check container health successfully")
    void testCheckContainerHealth() throws SshConnectionException, IOException {
        String mockInspect = "[{\"State\":{\"Status\":\"running\",\"ExitCode\":0,\"StartedAt\":\"2024-01-19T10:00:00Z\"},\"RestartCount\":0}]";
        String mockLogs = "2024-01-19 10:00:00 INFO: Started\n";

        when(sshManager.executeCommand("docker inspect myapp"))
            .thenReturn(mockInspect);
        when(sshManager.executeCommand("docker logs --tail 50 myapp"))
            .thenReturn(mockLogs);

        // Execute
        Map<String, Object> health = dockerService.checkContainerHealth("myapp");

        // Assert
        assertNotNull(health);
        assertEquals("running", health.get("status"));
        assertTrue((boolean) health.get("is_running"));
        assertEquals(0, health.get("restart_count"));
    }

    @Test
    @DisplayName("Should detect errors in container logs during health check")
    void testCheckContainerHealthWithErrors() throws SshConnectionException, IOException {
        String mockInspect = "[{\"State\":{\"Status\":\"running\",\"ExitCode\":0,\"StartedAt\":\"2024-01-19T10:00:00Z\"},\"RestartCount\":2}]";
        String mockLogs = "2024-01-19 10:00:00 INFO: Started\n" +
                         "2024-01-19 10:01:00 ERROR: Connection refused\n" +
                         "2024-01-19 10:02:00 FATAL: Database error\n";

        when(sshManager.executeCommand("docker inspect myapp"))
            .thenReturn(mockInspect);
        when(sshManager.executeCommand("docker logs --tail 50 myapp"))
            .thenReturn(mockLogs);

        // Execute
        Map<String, Object> health = dockerService.checkContainerHealth("myapp");

        // Assert
        assertNotNull(health);
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) health.get("recent_errors");
        assertEquals(2, errors.size());
        assertEquals(2, health.get("error_count"));
    }

    @Test
    @DisplayName("Should summarize all containers successfully")
    void testSummarizeAll() throws SshConnectionException, IOException {
        // Mock listContainers response
        String mockPs = "{\"ID\":\"abc123\",\"Names\":\"web-app\",\"Image\":\"web:latest\",\"State\":\"running\",\"Status\":\"Up 2h\",\"Ports\":\"\"}\n" +
                       "{\"ID\":\"def456\",\"Names\":\"db-app\",\"Image\":\"db:latest\",\"State\":\"exited\",\"Status\":\"Exited\",\"Ports\":\"\"}\n";

        // Mock docker inspect responses for each container
        String mockInspect1 = "[{\"State\":{\"Status\":\"running\",\"ExitCode\":0,\"StartedAt\":\"2024-01-19T10:00:00Z\"},\"RestartCount\":0}]";
        String mockInspect2 = "[{\"State\":{\"Status\":\"exited\",\"ExitCode\":1,\"StartedAt\":\"2024-01-19T08:00:00Z\"},\"RestartCount\":0}]";
        String mockLogs = "";

        // Setup mocks for listContainers call
        when(sshManager.executeCommand("docker ps -a --format '{{json .}}'"))
            .thenReturn(mockPs);

        // Setup mocks for checkContainerHealth calls
        when(sshManager.executeCommand("docker inspect web-app"))
            .thenReturn(mockInspect1);
        when(sshManager.executeCommand("docker logs --tail 50 web-app"))
            .thenReturn(mockLogs);

        when(sshManager.executeCommand("docker inspect db-app"))
            .thenReturn(mockInspect2);
        when(sshManager.executeCommand("docker logs --tail 50 db-app"))
            .thenReturn(mockLogs);

        // Execute
        Map<String, Object> summary = dockerService.summarizeAll();

        // Assert
        assertNotNull(summary);
        assertEquals(2, summary.get("total_containers"));
        assertEquals(1L, summary.get("running")); // Long type from stream().count()
        assertEquals(1L, summary.get("stopped")); // Long type from stream().count()
        assertTrue(summary.containsKey("containers"));
        assertTrue(summary.containsKey("problematic_containers"));
    }

    @Test
    @DisplayName("Should require container name parameter")
    void testGetContainerLogsRequiresName() {
        assertThrows(RuntimeException.class, () ->
            dockerService.getContainerLogs(null, 100, null));
    }

    @Test
    @DisplayName("Should use default tail value when not specified")
    void testGetContainerLogsDefaultTail() throws SshConnectionException, IOException {
        when(sshManager.executeCommand("docker logs --tail 100 myapp"))
            .thenReturn("logs");

        String logs = dockerService.getContainerLogs("myapp", 0, null);

        assertNotNull(logs);
    }
}


