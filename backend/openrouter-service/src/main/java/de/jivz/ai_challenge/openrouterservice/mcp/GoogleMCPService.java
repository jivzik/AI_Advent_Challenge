package de.jivz.ai_challenge.openrouterservice.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * MCP Service für Google Tasks Integration.
 * Wird nur aktiviert wenn die Konfiguration vorhanden ist.
 */
@Service
@Slf4j
public class GoogleMCPService extends BaseMCPService {

    public GoogleMCPService(
            @Value("${mcp.google.base-url:http://localhost:3001}") String baseUrl) {
        super(WebClient.create(baseUrl), "google");
        log.info("GoogleMCPService initialized with base URL: {}", baseUrl);
    }
}

