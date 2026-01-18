package de.jivz.agentservice.mcp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
@Service
@Slf4j
public class DockerMCPService extends BaseMCPService {
    public DockerMCPService(@Value("${mcp.docker.url:http://localhost:8083}") String baseUrl) {
        super(WebClient.create(baseUrl), "docker");
        log.info("DockerMCPService initialized with base URL: {}", baseUrl);
    }
}
