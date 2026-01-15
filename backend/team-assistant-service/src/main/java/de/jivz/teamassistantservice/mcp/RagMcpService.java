package de.jivz.teamassistantservice.mcp;



import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * MCP Service для RAG (Retrieval-Augmented Generation) сервера.
 *
 * Доступные инструменты:
 * - rag:search_documents - семантический поиск по документам
 * - rag:list_documents - список загруженных документов
 * - rag:get_document_info - информация о документе
 */
@Service
@Slf4j
public class RagMcpService extends BaseMCPService {

    public RagMcpService( @Value("${mcp.rag.base-url}") String baseUrl) {
        super(WebClient.create(baseUrl), "rag");
    }
}

