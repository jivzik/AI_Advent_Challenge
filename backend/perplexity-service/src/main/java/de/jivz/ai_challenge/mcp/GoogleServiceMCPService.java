package de.jivz.ai_challenge.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class GoogleServiceMCPService extends BaseMCPService{

    public GoogleServiceMCPService(@Qualifier("mcpWebClient") WebClient webClient) {
        super(webClient, "google_service");
    }
}
