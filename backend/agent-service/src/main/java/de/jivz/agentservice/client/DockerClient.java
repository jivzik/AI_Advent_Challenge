package de.jivz.agentservice.client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.agentservice.cli.domain.ContainerStatus;
import de.jivz.agentservice.mcp.DockerMCPService;
import de.jivz.agentservice.mcp.model.MCPToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Component
@Slf4j
@RequiredArgsConstructor
public class DockerClient {
    private final DockerMCPService dockerMCPService;
    private final ObjectMapper objectMapper;
    public Mono<List<ContainerStatus>> listContainers() {
        log.debug("Listing containers via MCP");
        return Mono.fromCallable(() -> {
            MCPToolResult result = dockerMCPService.execute("list_containers", Map.of());
            if (!result.isSuccess()) {
                log.error("Failed to list containers: {}", result.getError());
                return List.<ContainerStatus>of();
            }
            return parseContainerStatuses(result);
        });
    }

    public Mono<String> getContainerLogs(String containerName, int lines) {
        log.debug("Getting logs for container: {}", containerName);
        return Mono.fromCallable(() -> {
            MCPToolResult result = dockerMCPService.execute("get_container_logs", Map.of(
                "container_name", containerName,
                "lines", lines
            ));
            if (!result.isSuccess()) {
                log.error("Failed to get logs: {}", result.getError());
                return "Error retrieving logs: " + containerName;
            }

            Object resultObj = result.getResult();
            if (resultObj == null) {
                return "No logs available";
            }

            // Если результат String - вернуть как есть, иначе конвертировать
            return resultObj instanceof String ? (String) resultObj : resultObj.toString();
        });
    }

    public Mono<Boolean> restartContainer(String containerName) {
        log.info("Restarting container: {}", containerName);
        return Mono.fromCallable(() -> {
            MCPToolResult result = dockerMCPService.execute("restart_container", Map.of(
                "container_name", containerName
            ));
            if (!result.isSuccess()) {
                log.error("Failed to restart container: {}", result.getError());
                return false;
            }
            log.info("Container restarted: {}", containerName);
            return true;
        });
    }
    public Mono<Boolean> checkHealth(String serviceName) {
        log.debug("Checking health for: {}", serviceName);
        int port = getServicePort(serviceName);
        return Mono.fromCallable(() -> {
            try {
                MCPToolResult result = dockerMCPService.execute("check_health", Map.of(
                    "service_name", serviceName,
                    "port", port
                ));
                return result.isSuccess();
            } catch (Exception e) {
                log.warn("Health check failed for {}: {}", serviceName, e.getMessage());
                return false;
            }
        });
    }

    private List<ContainerStatus> parseContainerStatuses(MCPToolResult result) {
        List<ContainerStatus> statuses = new ArrayList<>();
        try {
            Object resultObj = result.getResult();
            if (resultObj == null) return statuses;

            // MCP возвращает List<Map<String, Object>>
            if (resultObj instanceof List<?> resultList) {
                for (Object item : resultList) {
                    if (item instanceof Map<?, ?>) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) item;
                        ContainerStatus status = parseContainerMap(map);
                        if (status != null) {
                            statuses.add(status);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse container statuses: {}", e.getMessage(), e);
        }
        return statuses;
    }

    private ContainerStatus parseContainerMap(Map<String, Object> map) {
        try {
            String name = (String) map.getOrDefault("name", "unknown");
            String status = (String) map.getOrDefault("status", "unknown");
            String state = (String) map.getOrDefault("state", "unknown");

            // Извлекаем uptime из status
            String uptime = extractUptime(status);

            ContainerStatus containerStatus = ContainerStatus.builder()
                .name(name)
                .status(state)
                .uptime(uptime)
                .memoryUsage(null)
                .image((String) map.getOrDefault("image", ""))
                .healthy("running".equalsIgnoreCase(state))
                .build();

            return containerStatus;
        } catch (Exception e) {
            log.warn("Failed to parse container map: {}", e.getMessage());
            return null;
        }
    }

    private String extractUptime(String status) {
        if (status == null) return "unknown";

        if (status.startsWith("Up ")) {
            String uptime = status.substring(3);
            int parenIndex = uptime.indexOf(" (");
            if (parenIndex > 0) {
                uptime = uptime.substring(0, parenIndex);
            }
            return uptime;
        } else if (status.startsWith("Exited")) {
            return status.toLowerCase();
        } else if (status.startsWith("Restarting")) {
            return "restarting";
        }

        return status;
    }
    private int getServicePort(String serviceName) {
        return switch (serviceName.toLowerCase()) {
            case "mcp-server" -> 8081;
            case "mcp-docker-monitor" -> 8083;
            case "openrouter-service" -> 8084;
            case "agent-service" -> 8085;
            case "rag-mcp-server", "rag-service" -> 8086;
            case "support-service" -> 8088;
            case "team-service", "team-assistant-service" -> 8089;
            default -> 8080;
        };
    }
}
