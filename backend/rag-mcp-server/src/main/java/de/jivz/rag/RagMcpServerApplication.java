package de.jivz.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RAG MCP Server Application
 *
 * Микросервис для RAG (Retrieval-Augmented Generation) системы:
 * - Загрузка документов (PDF, EPUB, TXT, MD, DOCX)
 * - Разбивка на чанки (chunking)
 * - Генерация эмбеддингов через OpenRouter API
 * - Хранение в PostgreSQL + pgvector
 * - MCP Tools для семантического поиска
 */
@SpringBootApplication
public class RagMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagMcpServerApplication.class, args);
    }
}

