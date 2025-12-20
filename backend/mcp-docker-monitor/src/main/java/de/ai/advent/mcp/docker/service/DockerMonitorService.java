package de.ai.advent.mcp.docker.service;

import de.ai.advent.mcp.docker.ssh.SshManager;
import de.ai.advent.mcp.docker.exception.SshConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Docker Monitoring Service via SSH
 *
 * Provides methods to monitor Docker containers and system information
 * on remote servers through SSH connections.
 */
@Service
public class DockerMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(DockerMonitorService.class);

    private final SshManager sshManager;

    public DockerMonitorService(SshManager sshManager) {
        this.sshManager = sshManager;
    }

    /**
     * Gets list of running Docker containers
     *
     * @return list of running container names
     */
    public List<String> getRunningContainers() {
        try {
            ensureConnection();
            String output = sshManager.executeCommand("docker ps --format '{{.Names}}'");
            return parseContainerOutput(output);
        } catch (Exception e) {
            logger.error("Failed to get running containers", e);
            return List.of();
        }
    }

    /**
     * Gets all Docker containers (running and stopped)
     *
     * @return list of all container names
     */
    public List<String> getAllContainers() {
        try {
            ensureConnection();
            String output = sshManager.executeCommand("docker ps -a --format '{{.Names}}'");
            return parseContainerOutput(output);
        } catch (Exception e) {
            logger.error("Failed to get all containers", e);
            return List.of();
        }
    }

    /**
     * Gets detailed information about a specific container
     *
     * @param containerId container name or ID
     * @return container inspection result
     */
    public Optional<String> inspectContainer(String containerId) {
        try {
            ensureConnection();
            String output = sshManager.executeCommand("docker inspect " + containerId);
            return Optional.of(output);
        } catch (Exception e) {
            logger.error("Failed to inspect container: {}", containerId, e);
            return Optional.empty();
        }
    }

    /**
     * Gets container logs
     *
     * @param containerId container name or ID
     * @param lines number of recent lines to retrieve (optional, 0 = all)
     * @return container logs
     */
    public Optional<String> getContainerLogs(String containerId, int lines) {
        try {
            ensureConnection();
            String command = lines > 0
                ? String.format("docker logs --tail %d %s", lines, containerId)
                : String.format("docker logs %s", containerId);
            String output = sshManager.executeCommand(command);
            return Optional.of(output);
        } catch (Exception e) {
            logger.error("Failed to get logs for container: {}", containerId, e);
            return Optional.empty();
        }
    }

    /**
     * Gets Docker system statistics
     *
     * @return Docker stats output
     */
    public Optional<String> getDockerStats() {
        try {
            ensureConnection();
            // Get stats with no-stream flag to get single snapshot
            String output = sshManager.executeCommand("docker stats --no-stream");
            return Optional.of(output);
        } catch (Exception e) {
            logger.error("Failed to get Docker statistics", e);
            return Optional.empty();
        }
    }

    /**
     * Gets Docker version and info
     *
     * @return Docker version information
     */
    public Optional<String> getDockerVersion() {
        try {
            ensureConnection();
            String output = sshManager.executeCommand("docker version");
            return Optional.of(output);
        } catch (Exception e) {
            logger.error("Failed to get Docker version", e);
            return Optional.empty();
        }
    }

    /**
     * Gets system information
     *
     * @return system info (uname output)
     */
    public Optional<String> getSystemInfo() {
        try {
            ensureConnection();
            String output = sshManager.executeCommand("uname -a");
            return Optional.of(output);
        } catch (Exception e) {
            logger.error("Failed to get system information", e);
            return Optional.empty();
        }
    }

    /**
     * Gets disk usage information
     *
     * @return disk usage in human-readable format
     */
    public Optional<String> getDiskUsage() {
        try {
            ensureConnection();
            String output = sshManager.executeCommand("df -h");
            return Optional.of(output);
        } catch (Exception e) {
            logger.error("Failed to get disk usage", e);
            return Optional.empty();
        }
    }

    /**
     * Gets memory and CPU usage
     *
     * @return top output
     */
    public Optional<String> getMemoryAndCpuUsage() {
        try {
            ensureConnection();
            String output = sshManager.executeCommand("free -h");
            return Optional.of(output);
        } catch (Exception e) {
            logger.error("Failed to get memory usage", e);
            return Optional.empty();
        }
    }

    /**
     * Starts a Docker container
     *
     * @param containerId container name or ID
     * @return true if successful
     */
    public boolean startContainer(String containerId) {
        try {
            ensureConnection();
            String output = sshManager.executeCommand("docker start " + containerId);
            logger.info("Started container: {} - {}", containerId, output);
            return true;
        } catch (Exception e) {
            logger.error("Failed to start container: {}", containerId, e);
            return false;
        }
    }

    /**
     * Stops a Docker container
     *
     * @param containerId container name or ID
     * @param timeout timeout in seconds
     * @return true if successful
     */
    public boolean stopContainer(String containerId, int timeout) {
        try {
            ensureConnection();
            String output = sshManager.executeCommand("docker stop -t " + timeout + " " + containerId);
            logger.info("Stopped container: {} - {}", containerId, output);
            return true;
        } catch (Exception e) {
            logger.error("Failed to stop container: {}", containerId, e);
            return false;
        }
    }

    /**
     * Restarts a Docker container
     *
     * @param containerId container name or ID
     * @return true if successful
     */
    public boolean restartContainer(String containerId) {
        try {
            ensureConnection();
            String output = sshManager.executeCommand("docker restart " + containerId);
            logger.info("Restarted container: {} - {}", containerId, output);
            return true;
        } catch (Exception e) {
            logger.error("Failed to restart container: {}", containerId, e);
            return false;
        }
    }

    /**
     * Removes a Docker container
     *
     * @param containerId container name or ID
     * @param force force removal (kill running container)
     * @return true if successful
     */
    public boolean removeContainer(String containerId, boolean force) {
        try {
            ensureConnection();
            String cmd = force
                ? "docker rm -f " + containerId
                : "docker rm " + containerId;
            String output = sshManager.executeCommand(cmd);
            logger.info("Removed container: {} - {}", containerId, output);
            return true;
        } catch (Exception e) {
            logger.error("Failed to remove container: {}", containerId, e);
            return false;
        }
    }

    /**
     * Executes a custom command on the remote server
     *
     * @param command command to execute
     * @return command output
     */
    public Optional<String> executeCustomCommand(String command) {
        try {
            ensureConnection();
            String output = sshManager.executeCommand(command);
            return Optional.of(output);
        } catch (Exception e) {
            logger.error("Failed to execute custom command: {}", command, e);
            return Optional.empty();
        }
    }

    /**
     * Ensures SSH connection is active
     *
     * @throws SshConnectionException if connection fails
     */
    private void ensureConnection() throws SshConnectionException {
        if (!sshManager.isConnected()) {
            logger.debug("SSH connection not active, attempting to connect...");
            sshManager.connect();
        }
    }

    /**
     * Parses container output into list of names
     *
     * @param output raw output from docker command
     * @return list of container names
     */
    private List<String> parseContainerOutput(String output) {
        return Arrays.stream(output.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Health check - tests SSH connection and Docker availability
     *
     * @return true if system is healthy
     */
    public boolean healthCheck() {
        try {
            ensureConnection();
            String output = sshManager.executeCommand("docker ps -q");
            logger.debug("Health check passed");
            return true;
        } catch (Exception e) {
            logger.warn("Health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Cleanup - disconnects SSH session
     */
    public void cleanup() {
        sshManager.disconnect();
        logger.info("Docker monitor service cleanup completed");
    }
}

