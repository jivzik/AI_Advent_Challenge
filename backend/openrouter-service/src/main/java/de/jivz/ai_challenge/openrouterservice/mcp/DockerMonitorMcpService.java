package de.jivz.ai_challenge.openrouterservice.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class DockerMonitorMcpService extends BaseMCPService {

    public DockerMonitorMcpService( @Value("${mcp.docker.monitor.base-url}") String baseUrl) {
        super(WebClient.create(baseUrl), "docker");
    }
}
