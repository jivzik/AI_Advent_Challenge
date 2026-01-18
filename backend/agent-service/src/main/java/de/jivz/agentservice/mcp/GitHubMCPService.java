package de.jivz.agentservice.mcp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
@Service
@Slf4j
public class GitHubMCPService extends BaseMCPService {
    public GitHubMCPService(@Value("${mcp.server.url:http://localhost:8081}") String baseUrl) {
        super(WebClient.create(baseUrl), "github");
        log.info("GitHubMCPService initialized with base URL: {}", baseUrl);
    }
}
