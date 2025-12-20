package de.ai.advent.mcp.docker.service;

import de.ai.advent.mcp.docker.exception.DockerCommandException;
import de.ai.advent.mcp.docker.exception.SshConnectionException;
import de.ai.advent.mcp.docker.model.ContainerInfo;
import de.ai.advent.mcp.docker.ssh.SshManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Docker Service - инкапсулирует всю логику работы с Docker через SSH
 *
 * Методы:
 * - listContainers() - получить список всех контейнеров
 * - getContainerLogs() - получить логи контейнера
 * - checkContainerHealth() - проверить здоровье контейнера
 * - summarizeAll() - получить общую статистику по всем контейнерам
 */
@Slf4j
@Service
public class DockerService {

    private final SshManager sshManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int DEFAULT_LOG_TAIL = 100;
    private static final int HEALTH_CHECK_TAIL = 50;

    public DockerService(SshManager sshManager) {
        this.sshManager = sshManager;
    }

    /**
     * Получить список всех контейнеров
     * Выполняет: docker ps -a --format "{{json .}}"
     *
     * @return список ContainerInfo с ID, Name, Status, State, Uptime, Image
     * @throws DockerCommandException если команда Docker не выполнилась
     * @throws SshConnectionException если SSH не подключился
     */
    public List<ContainerInfo> listContainers() {
        try {
            log.debug("Executing: docker ps -a --format '{{json .}}'");
            String output = sshManager.executeCommand("docker ps -a --format '{{json .}}'");

            if (output == null || output.trim().isEmpty()) {
                log.warn("No containers found");
                return new ArrayList<>();
            }

            List<ContainerInfo> containers = parseContainersFromJsonLines(output);
            log.info("Retrieved {} containers from Docker", containers.size());
            return containers;

        } catch (SshConnectionException e) {
            log.error("SSH connection error while listing containers", e);
            throw e;
        } catch (IOException e) {
            log.error("IO error while listing containers", e);
            throw new DockerCommandException("Failed to list containers", e.getMessage());
        }
    }

    /**
     * Получить логи контейнера
     * Выполняет: docker logs --tail {tail} [--since {since}] {containerName}
     *
     * @param containerName имя или ID контейнера
     * @param tail количество последних строк (default: 100)
     * @param since временной фильтр (например: "1h", "30m", опционально)
     * @return логи контейнера
     * @throws DockerCommandException если контейнер не найден или команда не выполнилась
     * @throws SshConnectionException если SSH не подключился
     */
    public String getContainerLogs(String containerName, int tail, String since) {
        try {
            if (containerName == null || containerName.trim().isEmpty()) {
                throw new IllegalArgumentException("Container name is required");
            }

            int logTail = tail > 0 ? tail : DEFAULT_LOG_TAIL;

            StringBuilder command = new StringBuilder("docker logs --tail ").append(logTail);

            if (since != null && !since.trim().isEmpty()) {
                command.append(" --since ").append(since);
            }

            command.append(" ").append(containerName);

            log.debug("Executing: {}", command);
            String output = sshManager.executeCommand(command.toString());

            if (output == null) {
                throw new DockerCommandException("Container not found: " + containerName);
            }

            log.info("Retrieved logs for container: {} ({} bytes)", containerName, output.length());
            return output;

        } catch (SshConnectionException e) {
            log.error("SSH connection error while getting logs for container: {}", containerName, e);
            throw e;
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("No such container")) {
                throw new DockerCommandException("Container not found: " + containerName);
            }
            log.error("IO error while getting logs for container: {}", containerName, e);
            throw new DockerCommandException("Failed to get container logs", e.getMessage());
        }
    }

    /**
     * Проверить здоровье контейнера
     * Выполняет: docker inspect {containerName}
     * Парсит JSON и проверяет:
     * - Status, State (running/exited)
     * - RestartCount
     * - Health status (если есть healthcheck)
     * - Exit code и ошибки (если упал)
     * - Ищет ERROR/FATAL в последних 50 строках логов
     *
     * @param containerName имя или ID контейнера
     * @return Map с информацией о здоровье контейнера
     * @throws RuntimeException если контейнер не найден или команда не выполнилась
     */
    public Map<String, Object> checkContainerHealth(String containerName) {
        Map<String, Object> health = new HashMap<>();

        try {
            if (containerName == null || containerName.trim().isEmpty()) {
                throw new RuntimeException("Container name is required");
            }

            log.debug("Checking health for container: {}", containerName);

            // Получить информацию о контейнере через docker inspect
            String inspectOutput = sshManager.executeCommand("docker inspect " + containerName);

            if (inspectOutput == null || inspectOutput.trim().isEmpty()) {
                throw new RuntimeException("Container not found: " + containerName);
            }

            JsonNode containers = objectMapper.readTree(inspectOutput);

            if (!containers.isArray() || containers.size() == 0) {
                throw new RuntimeException("Container not found: " + containerName);
            }

            JsonNode container = containers.get(0);

            // Базовая информация
            String status = container.get("State").get("Status").asText();
            boolean isRunning = "running".equals(status);
            int restartCount = container.get("RestartCount").asInt();
            int exitCode = container.get("State").get("ExitCode").asInt();
            String startedAt = container.get("State").get("StartedAt").asText();

            health.put("container_name", containerName);
            health.put("status", status);
            health.put("is_running", isRunning);
            health.put("restart_count", restartCount);
            health.put("exit_code", exitCode);
            health.put("started_at", startedAt);

            // Проверить Health status (если есть healthcheck)
            if (container.has("State") && container.get("State").has("Health")) {
                JsonNode healthNode = container.get("State").get("Health");
                if (healthNode != null) {
                    String health_status = healthNode.get("Status").asText();
                    health.put("health_status", health_status);
                    log.debug("Container {} health status: {}", containerName, health_status);
                }
            }

            // Если контейнер упал - добавить причину
            if (!isRunning && exitCode != 0) {
                health.put("failure_reason", "Exited with code: " + exitCode);
            }

            // Проверить логи на ошибки
            try {
                String logs = getContainerLogs(containerName, HEALTH_CHECK_TAIL, null);
                List<String> errors = Arrays.stream(logs.split("\n"))
                        .filter(line -> line.toUpperCase().contains("ERROR") ||
                                       line.toUpperCase().contains("FATAL") ||
                                       line.toUpperCase().contains("EXCEPTION"))
                        .limit(10)
                        .collect(Collectors.toList());

                health.put("recent_errors", errors);
                health.put("error_count", errors.size());

                if (!errors.isEmpty()) {
                    health.put("health_check", "FAILED - found " + errors.size() + " errors in logs");
                } else {
                    health.put("health_check", isRunning ? "HEALTHY" : "UNKNOWN");
                }

            } catch (Exception e) {
                log.warn("Failed to check logs for container {}: {}", containerName, e.getMessage());
                health.put("health_check", isRunning ? "RUNNING" : "STOPPED");
            }

            log.debug("Health check completed for container: {}", containerName);
            return health;

        } catch (SshConnectionException e) {
            log.error("SSH connection failed", e);
            throw e;
        } catch (IOException e) {
            log.error("Failed to parse container info", e);
            throw new DockerCommandException("Failed to check container health", e.getMessage());
        }
    }

    /**
     * Получить сводку по всем контейнерам
     * Вызывает listContainers() и группирует контейнеры по статусу
     * Для проблемных контейнеров добавляет описание проблемы
     *
     * @return Map с общей статистикой и деталями
     */
    public Map<String, Object> summarizeAll() {
        try {
            log.debug("Summarizing all containers");

            Map<String, Object> summary = new HashMap<>();
            List<ContainerInfo> allContainers = listContainers();

            // Статистика
            long runningCount = allContainers.stream()
                    .filter(c -> "running".equalsIgnoreCase(c.getState()))
                    .count();

            long stoppedCount = allContainers.stream()
                    .filter(c -> "exited".equalsIgnoreCase(c.getState()))
                    .count();

            summary.put("total_containers", allContainers.size());
            summary.put("running", runningCount);
            summary.put("stopped", stoppedCount);

            // Все контейнеры
            List<Map<String, Object>> allContainersList = new ArrayList<>();

            // Проблемные контейнеры
            List<Map<String, Object>> problematicContainers = new ArrayList<>();

            // Проверить каждый контейнер
            for (ContainerInfo container : allContainers) {
                Map<String, Object> containerInfo = new HashMap<>();
                containerInfo.put("id", container.getContainerId());
                containerInfo.put("name", container.getName());
                containerInfo.put("image", container.getImage());
                containerInfo.put("status", container.getStatus());
                containerInfo.put("state", container.getState());

                allContainersList.add(containerInfo);

                // Проверить здоровье контейнера
                try {
                    Map<String, Object> health = checkContainerHealth(container.getName());

                    boolean isHealthy = true;
                    String problemDescription = null;

                    // Контейнер упал
                    if (health.containsKey("failure_reason")) {
                        isHealthy = false;
                        problemDescription = (String) health.get("failure_reason");
                    }

                    // Высокий restart count
                    int restarts = (int) health.getOrDefault("restart_count", 0);
                    if (restarts > 0) {
                        problemDescription = "Restarted " + restarts + " times";
                    }

                    // Ошибки в логах
                    @SuppressWarnings("unchecked")
                    List<String> errors = (List<String>) health.get("recent_errors");
                    if (errors != null && !errors.isEmpty()) {
                        isHealthy = false;
                        if (problemDescription == null) {
                            problemDescription = "Found " + errors.size() + " errors in logs";
                        }
                    }

                    if (!isHealthy && problemDescription != null) {
                        Map<String, Object> problem = new HashMap<>();
                        problem.put("container", container.getName());
                        problem.put("issue", problemDescription);
                        problem.put("details", health);
                        problematicContainers.add(problem);
                    }

                } catch (Exception e) {
                    log.warn("Failed to check health for container {}: {}", container.getName(), e.getMessage());
                    Map<String, Object> problem = new HashMap<>();
                    problem.put("container", container.getName());
                    problem.put("issue", "Health check failed: " + e.getMessage());
                    problematicContainers.add(problem);
                }
            }

            summary.put("containers", allContainersList);
            summary.put("problematic_containers", problematicContainers);
            summary.put("problem_count", problematicContainers.size());

            log.info("Summary completed: {} total, {} running, {} stopped, {} problematic",
                    allContainers.size(), runningCount, stoppedCount, problematicContainers.size());

            return summary;

        } catch (Exception e) {
            log.error("Failed to summarize containers", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("success", false);
            return errorResponse;
        }
    }

    /**
     * Парсит JSON-строки в объекты ContainerInfo
     * Формат: docker ps -a --format "{{json .}}"
     *
     * @param output JSON строки, одна строка на контейнер
     * @return список ContainerInfo
     */
    private List<ContainerInfo> parseContainersFromJsonLines(String output) throws IOException {
        List<ContainerInfo> containers = new ArrayList<>();

        for (String line : output.split("\n")) {
            if (line.trim().isEmpty()) {
                continue;
            }

            try {
                JsonNode node = objectMapper.readTree(line);

                String id = node.has("ID") ? node.get("ID").asText() : "";
                String name = node.has("Names") ? node.get("Names").asText() : "";
                String image = node.has("Image") ? node.get("Image").asText() : "";
                String status = node.has("Status") ? node.get("Status").asText() : "";
                String state = node.has("State") ? node.get("State").asText() : "";
                String ports = node.has("Ports") ? node.get("Ports").asText() : "";
                String created = node.has("CreatedAt") ? node.get("CreatedAt").asText() : "";

                ContainerInfo info = new ContainerInfo(id, name, image, status, state, ports, created);
                containers.add(info);

                log.debug("Parsed container: {} ({})", name, id);

            } catch (Exception e) {
                log.warn("Failed to parse container line: {}", line, e);
            }
        }

        return containers;
    }

    /**
     * Получить информацию о портах из JSON узла (старый метод, оставлен для совместимости)
     *
     * @param node JSON узел контейнера
     * @return строка с портами
     */
    private String getPortsInfo(JsonNode node) {
        try {
            if (node.has("NetworkSettings") && node.get("NetworkSettings").has("Ports")) {
                JsonNode ports = node.get("NetworkSettings").get("Ports");
                if (ports != null && ports.isObject()) {
                    StringBuilder sb = new StringBuilder();
                    ports.fields().forEachRemaining(entry -> {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(entry.getKey());
                    });
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse ports info", e);
        }
        return "";
    }

    /**
     * Старый метод парсинга контейнеров (для совместимости)
     *
     * @param output JSON строки
     * @return список ContainerInfo
     */
    private List<ContainerInfo> parseContainers(String output) throws IOException {
        return parseContainersFromJsonLines(output);
    }
}

