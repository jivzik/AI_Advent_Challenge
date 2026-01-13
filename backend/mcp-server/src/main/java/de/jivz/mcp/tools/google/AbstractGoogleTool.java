package de.jivz.mcp.tools.google;

import de.jivz.mcp.tools.Tool;
import de.jivz.mcp.tools.ToolExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractGoogleTool implements Tool {

    protected final WebClient gsWebClient;
    protected final ThreadLocal<String> contextTaskListId = new ThreadLocal<>();

    protected Object callGoogleService(String url) {
        try {
            log.debug("Google Service aufrufen: GET {}", url);
            Object response = gsWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return buildSuccessResponse(response, null);
        } catch (Exception e) {
            log.error("Fehler beim Google Service Aufruf: {}", e.getMessage());
            throw new ToolExecutionException("Google Service Fehler: " + e.getMessage(), e);
        }
    }

    protected Object callGoogleServicePost(String url, Map<String, Object> body) {
        try {
            log.debug("Google Service aufrufen: POST {} mit Body: {}", url, body);
            Object response = gsWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return response;
        } catch (Exception e) {
            log.error("Fehler beim Google Service Aufruf: {}", e.getMessage());
            throw new ToolExecutionException("Google Service Fehler: " + e.getMessage(), e);
        }
    }

    protected Object callGoogleServicePatch(String url, Map<String, Object> body) {
        try {
            log.debug("Google Service aufrufen: PATCH {} mit Body: {}", url, body);
            Object response = gsWebClient.patch()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();
            return response;
        } catch (Exception e) {
            log.error("Fehler beim Google Service Aufruf: {}", e.getMessage());
            throw new ToolExecutionException("Google Service Fehler: " + e.getMessage(), e);
        }
    }

    protected Object callGoogleServiceDelete(String url) {
        try {
            log.debug("Google Service aufrufen: DELETE {}", url);
            gsWebClient.delete()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            return buildSuccessResponse(Map.of("message", "Erfolgreich gelöscht"), null);
        } catch (Exception e) {
            log.error("Fehler beim Google Service Aufruf: {}", e.getMessage());
            throw new ToolExecutionException("Google Service Fehler: " + e.getMessage(), e);
        }
    }

    protected Map<String, Object> buildSuccessResponse(Object result, String taskListId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("result", result);
        if (taskListId != null && !taskListId.isBlank()) {
            response.put("taskListId", taskListId);
        }
        return response;
    }
}

