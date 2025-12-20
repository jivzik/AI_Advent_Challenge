package de.jivz.ai_challenge.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class DockerMonitorMcpService extends BaseMCPService {

    public DockerMonitorMcpService(@Qualifier("mcpDockerMonitorWebClient") WebClient webClient) {
        super(webClient, "docker");
    }
}
