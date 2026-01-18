# RAG MCP Server Integration

## 📋 Quick Summary
Der RAG MCP Server ist ein spezialisierter Microservice für Retrieval-Augmented Generation (RAG), der semantische Dokumentensuche mit PostgreSQL pgvector, OpenRouter Embeddings und MCP Tool-Integration bereitstellt. Der Service ermöglicht intelligente Dokumentenindexierung, Chunking und Vektorsuche für LLM-Anwendungen.

## 🎯 Use Cases
- **Use Case 1**: Semantische Dokumentensuche in PDF, EPUB und TXT-Dateien für Chatbots
- **Use Case 2**: Kontextbasierte Antworten durch RAG-Integration mit OpenRouter Service
- **Use Case 3**: Multi-Format-Dokumentenindexierung mit automatischem Chunking und Embedding-Generierung
- **Use Case 4**: MCP Tool-basierte Dokumentenverwaltung und -suche für externe Services

## 🏗️ Architecture Overview

### High-Level Diagram (ASCII)
```
OpenRouter Service (port 8084)
         │
         ↓ MCP Tools
RAG MCP Server (port 8086)
         │
    ┌────┴────┐
    │         │
[Documents] [Search]
    │         │
    ↓         ↓
┌─────────────────────────────┐
│ Processing Pipeline:         │
│ 1. Document Upload           │
│ 2. Chunking (500 tokens)     │
│ 3. OpenRouter Embeddings     │
│ 4. pgvector Storage          │
│ 5. Semantic Search           │
└─────────────────────────────┘
         │
         ↓
PostgreSQL + pgvector
(Vector similarity search)
```

### Key Components

1. **RagService** (`backend/rag-mcp-server/src/main/java/de/jivz/rag/service/RagService.java`)
   - Purpose: Main RAG orchestration service
   - Methods: `uploadDocument()`, `searchDocuments()`, `hybridSearch()`
   - Dependencies: ChunkingService, EmbeddingService, DocumentRepository

2. **EmbeddingService** (`backend/rag-mcp-server/src/main/java/de/jivz/rag/service/EmbeddingService.java`)
   - Purpose: Generates vector embeddings via OpenRouter API
   - Model: `text-embedding-3-small` (1536 dimensions)
   - Dependencies: WebClient to OpenRouter

3. **ChunkingService** (`backend/rag-mcp-server/src/main/java/de/jivz/rag/service/ChunkingService.java`)
   - Purpose: Splits documents into searchable chunks
   - Strategy: 500 tokens per chunk with 50 token overlap
   - Used by: RagService during document upload

4. **DocumentChunkRepository** (`backend/rag-mcp-server/src/main/java/de/jivz/rag/repository/DocumentChunkRepository.java`)
   - Purpose: pgvector queries for semantic search
   - Methods: `searchByEmbedding()`, `searchByKeywords()`, `hybridSearch()`
   - Technology: JPA with native pgvector queries

5. **McpToolsController** (`backend/rag-mcp-server/src/main/java/de/jivz/rag/controller/McpToolsController.java`)
   - Purpose: MCP protocol endpoints for tool integration
   - Endpoints: `/api/mcp/tools`, `/api/mcp/tools/execute`
   - Dependencies: ToolExecutorService

## 📊 MCP Tools

### Tool 1: rag:search_documents
Performs semantic search across all indexed documents.

**Parameters:**
- `query` (required): Search query text
- `topK` (optional): Number of results, default 5, max 20

**Response:**
```json
{
  "results": [
    {
      "documentName": "Spring_Boot_Guide.pdf",
      "chunkText": "Spring Boot is a framework...",
      "relevance": 0.89,
      "chunkIndex": 3,
      "metadata": {"page": 5}
    }
  ],
  "totalResults": 5
}
```

### Tool 2: rag:list_documents
Lists all uploaded documents.

**Parameters:** None

**Response:**
```json
{
  "documents": [
    {
      "id": 123,
      "name": "Spring_Boot_Guide.pdf",
      "chunkCount": 45,
      "uploadedAt": "2026-01-13T10:00:00Z"
    }
  ]
}
```

### Tool 3: rag:get_document_info
Retrieves detailed information about a specific document.

**Parameters:**
- `documentId` (required): Document ID

**Response:**
```json
{
  "id": 123,
  "name": "Spring_Boot_Guide.pdf",
  "chunkCount": 45,
  "totalTokens": 22500,
  "uploadedAt": "2026-01-13T10:00:00Z",
  "metadata": {
    "fileSize": 2457600,
    "format": "PDF"
  }
}
```

## 💻 Complete Code Examples

### Example 1: Document Upload and Indexing

```java
// File: backend/rag-mcp-server/src/main/java/de/jivz/rag/service/RagService.java
@Service
@Slf4j
public class RagService {
    
    private final DocumentParserService parserService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    
    public DocumentDto uploadDocument(MultipartFile file) {
        log.info("Uploading document: {}", file.getOriginalFilename());
        
        // 1. Parse document content
        String content = parserService.parseDocument(file);
        
        // 2. Create document entity
        Document document = new Document();
        document.setName(file.getOriginalFilename());
        document.setUploadedAt(LocalDateTime.now());
        document = documentRepository.save(document);
        
        // 3. Split into chunks (500 tokens, 50 overlap)
        List<String> chunks = chunkingService.chunkText(content, 500, 50);
        log.info("Created {} chunks for document {}", chunks.size(), document.getId());
        
        // 4. Generate embeddings and save chunks
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            
            // Generate embedding vector
            float[] embedding = embeddingService.generateEmbedding(chunkText);
            
            // Create chunk entity
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(document);
            chunk.setChunkIndex(i);
            chunk.setChunkText(chunkText);
            chunk.setEmbedding(embedding);
            chunk.setTokenCount(chunkingService.countTokens(chunkText));
            
            chunkRepository.save(chunk);
        }
        
        log.info("Successfully indexed document {}", document.getId());
        
        return DocumentDto.fromEntity(document, chunks.size());
    }
}
```

**Explanation:**
- **Multi-format parsing**: Supports PDF, EPUB, TXT via Apache Tika
- **Smart chunking**: 500 tokens with 50 overlap for context preservation
- **Embedding generation**: Uses OpenRouter text-embedding-3-small model
- **pgvector storage**: Embeddings stored as float arrays in PostgreSQL

### Example 2: Semantic Search with pgvector

```java
// File: backend/rag-mcp-server/src/main/java/de/jivz/rag/repository/DocumentChunkRepository.java
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    
    /**
     * Semantic search using cosine similarity in pgvector
     */
    @Query(value = """
        SELECT dc.*, 
               1 - (dc.embedding <=> CAST(:queryEmbedding AS vector)) AS similarity
        FROM document_chunks dc
        WHERE 1 - (dc.embedding <=> CAST(:queryEmbedding AS vector)) > :threshold
        ORDER BY similarity DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> searchByEmbedding(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("threshold") double threshold,
        @Param("limit") int limit
    );
    
    /**
     * Hybrid search: semantic + keyword matching
     */
    @Query(value = """
        SELECT dc.*, 
               (0.7 * (1 - (dc.embedding <=> CAST(:queryEmbedding AS vector))) +
                0.3 * ts_rank_cd(dc.text_vector, plainto_tsquery('russian', :keywords))) AS score
        FROM document_chunks dc
        WHERE dc.text_vector @@ plainto_tsquery('russian', :keywords)
           OR (1 - (dc.embedding <=> CAST(:queryEmbedding AS vector))) > 0.7
        ORDER BY score DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> hybridSearch(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("keywords") String keywords,
        @Param("limit") int limit
    );
}
```

**Explanation:**
- **Cosine similarity**: `<=>` operator for vector distance in pgvector
- **Threshold filtering**: Only returns results with similarity > 0.7
- **Hybrid search**: Combines semantic (70%) and keyword (30%) matching
- **Full-text search**: Uses PostgreSQL tsvector for Russian language support

### Example 3: MCP Tool Integration

```bash
# File: N/A (curl command)
# Execute RAG search tool from OpenRouter Service

# 1. Upload a document first
curl -X POST "http://localhost:8086/api/documents/upload" \
  -F "file=@Spring_Boot_Guide.pdf"

# 2. Search via MCP tool
curl -X POST "http://localhost:8086/api/mcp/tools/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "rag:search_documents",
    "arguments": {
      "query": "How to configure Spring Boot application properties?",
      "topK": 5
    }
  }'

# Response:
# {
#   "success": true,
#   "result": {
#     "results": [
#       {
#         "documentName": "Spring_Boot_Guide.pdf",
#         "chunkText": "Application properties can be configured...",
#         "relevance": 0.89,
#         "chunkIndex": 12
#       }
#     ],
#     "totalResults": 5
#   }
# }
```

## 📂 File Structure

```
backend/rag-mcp-server/
├── pom.xml                                    # Maven dependencies (pgvector, Apache Tika)
├── README.md                                  # Service documentation
└── src/main/
    ├── java/de/jivz/rag/
    │   ├── RagMcpServerApplication.java       # Spring Boot main class
    │   ├── controller/
    │   │   ├── DocumentController.java        # REST API for document CRUD
    │   │   ├── SearchController.java          # Search endpoints
    │   │   └── McpToolsController.java        # MCP protocol endpoints
    │   ├── dto/
    │   │   ├── DocumentDto.java               # Document DTO
    │   │   ├── SearchRequest.java             # Search request DTO
    │   │   ├── SearchResultDto.java           # Search result DTO
    │   │   └── ChunkDto.java                  # Chunk DTO
    │   ├── entity/
    │   │   ├── Document.java                  # JPA entity for documents
    │   │   └── DocumentChunk.java             # JPA entity with pgvector
    │   ├── mcp/
    │   │   ├── McpModels.java                 # MCP protocol models
    │   │   ├── ToolsDefinitionService.java    # Tool definitions
    │   │   └── ToolExecutorService.java       # Tool execution logic
    │   ├── repository/
    │   │   ├── DocumentRepository.java        # JPA repository for documents
    │   │   └── DocumentChunkRepository.java   # Custom pgvector queries
    │   └── service/
    │       ├── RagService.java                # Main RAG orchestration
    │       ├── ChunkingService.java           # Text chunking logic
    │       ├── DocumentParserService.java     # Multi-format parsing
    │       ├── EmbeddingService.java          # OpenRouter embedding client
    │       └── KeywordSearchService.java      # Full-text search
    └── resources/
        ├── application.yml                    # Main configuration
        └── db/migration/
            ├── V1__init_schema.sql            # Initial database schema
            └── V2__add_fts_support.sql        # Full-text search support

backend/openrouter-service/src/main/java/de/jivz/openrouter/mcp/
└── RagMcpService.java                         # MCP client for RAG integration
```

## 🔌 API Reference

### POST /api/documents/upload
Upload and index a document.

**Request:**
```bash
curl -X POST "http://localhost:8086/api/documents/upload" \
  -F "file=@document.pdf"
```

**Response:**
```json
{
  "id": 123,
  "name": "document.pdf",
  "chunkCount": 45,
  "uploadedAt": "2026-01-13T10:00:00Z"
}
```

**Supported Formats:**
- PDF (`.pdf`)
- EPUB (`.epub`)
- Plain Text (`.txt`, `.md`)

**Status Codes:**
- 200: Success
- 400: Invalid file format
- 500: Processing error

### POST /api/documents/search
Semantic search across all documents.

**Request:**
```bash
curl -X POST "http://localhost:8086/api/documents/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Spring Boot configuration",
    "topK": 5,
    "threshold": 0.7
  }'
```

**Response:**
```json
{
  "query": "Spring Boot configuration",
  "results": [
    {
      "documentId": 123,
      "documentName": "Spring_Boot_Guide.pdf",
      "chunkText": "Spring Boot applications can be configured...",
      "relevance": 0.89,
      "chunkIndex": 12
    }
  ],
  "totalResults": 5,
  "processingTime": "120ms"
}
```

**Parameters:**
- `query` (required): Search query text
- `topK` (optional): Number of results, default 5
- `threshold` (optional): Minimum similarity, default 0.7

### GET /api/documents
List all uploaded documents.

**Request:**
```bash
curl "http://localhost:8086/api/documents"
```

**Response:**
```json
{
  "documents": [
    {
      "id": 123,
      "name": "Spring_Boot_Guide.pdf",
      "chunkCount": 45,
      "uploadedAt": "2026-01-13T10:00:00Z"
    }
  ],
  "totalDocuments": 1
}
```

### POST /api/mcp/tools/execute
Execute MCP tool (for integration with OpenRouter Service).

**Request:**
```bash
curl -X POST "http://localhost:8086/api/mcp/tools/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "rag:search_documents",
    "arguments": {
      "query": "microservices architecture",
      "topK": 3
    }
  }'
```

**Response:**
```json
{
  "success": true,
  "result": {
    "results": [...],
    "totalResults": 3
  }
}
```

## ⚙️ Configuration

### Required Properties

File: `backend/rag-mcp-server/src/main/resources/application.yml`

```yaml
server:
  port: 8086

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rag_db
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

# OpenRouter API for embeddings
openrouter:
  api-key: ${OPENROUTER_API_KEY}
  base-url: https://openrouter.ai/api/v1
  embedding-model: openai/text-embedding-3-small
  embedding-dimensions: 1536
```

### Optional Properties

```yaml
# Chunking configuration
chunking:
  chunk-size: 500           # Tokens per chunk
  overlap: 50               # Overlap tokens
  
# Search configuration
search:
  default-top-k: 5
  default-threshold: 0.7
  max-top-k: 20

# Logging
logging:
  level:
    de.jivz.rag: DEBUG
    org.hibernate.SQL: DEBUG
```

### Environment Variables

```bash
# Required
export OPENROUTER_API_KEY='sk-or-v1-your-key-here'

# Optional - Database
export DATABASE_URL='jdbc:postgresql://localhost:5432/rag_db'
export DATABASE_USER='postgres'
export DATABASE_PASSWORD='postgres'
```

### PostgreSQL Setup with pgvector

```bash
# Pull pgvector-enabled PostgreSQL image
docker pull pgvector/pgvector:pg16

# Run container
docker run -d \
  --name rag-postgres \
  -e POSTGRES_DB=rag_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  pgvector/pgvector:pg16

# Verify pgvector extension
docker exec -it rag-postgres psql -U postgres -d rag_db \
  -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

## 🚀 Quick Start Guide

### Step 1: Set Up PostgreSQL with pgvector

```bash
# Start PostgreSQL container
docker run -d \
  --name rag-postgres \
  -e POSTGRES_DB=rag_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  pgvector/pgvector:pg16

# Wait for container to be ready
sleep 5

# Create pgvector extension
docker exec -it rag-postgres psql -U postgres -d rag_db \
  -c "CREATE EXTENSION vector;"
```

### Step 2: Initialize Database Schema

```bash
cd backend/rag-mcp-server

# Run Flyway migration (automatically on startup)
# Or manually:
psql -h localhost -U postgres -d rag_db \
  -f src/main/resources/db/migration/V1__init_schema.sql
```

### Step 3: Set Environment Variables

```bash
# Set OpenRouter API key
export OPENROUTER_API_KEY='sk-or-v1-your-key-here'

# Verify
echo $OPENROUTER_API_KEY
```

### Step 4: Build and Run

```bash
# Build the service
mvn clean install -DskipTests

# Run the service
mvn spring-boot:run

# Expected output:
# Started RagMcpServerApplication in X.XXX seconds
# Server listening on port 8086
```

### Step 5: Verify Installation

```bash
# Check health
curl http://localhost:8086/actuator/health

# Upload test document
curl -X POST "http://localhost:8086/api/documents/upload" \
  -F "file=@test.txt"

# Test search
curl -X POST "http://localhost:8086/api/documents/search" \
  -H "Content-Type: application/json" \
  -d '{"query":"test query","topK":5}' | jq

# Test MCP tools
curl "http://localhost:8086/api/mcp/tools" | jq
```

## 🧪 Testing

### Manual Testing Script

```bash
#!/bin/bash
# File: test-rag-mcp.sh

BASE_URL="http://localhost:8086/api"

echo "=== RAG MCP Server Test Suite ==="

# Test 1: Upload document
echo "1. Uploading test document..."
UPLOAD_RESPONSE=$(curl -s -X POST "$BASE_URL/documents/upload" \
  -F "file=@test-document.pdf")
DOC_ID=$(echo $UPLOAD_RESPONSE | jq -r '.id')
echo "Document ID: $DOC_ID"

# Test 2: List documents
echo "2. Listing all documents..."
curl -s "$BASE_URL/documents" | jq

# Test 3: Semantic search
echo "3. Testing semantic search..."
curl -s -X POST "$BASE_URL/documents/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query":"Spring Boot configuration",
    "topK":5
  }' | jq

# Test 4: MCP tool execution
echo "4. Testing MCP tool..."
curl -s -X POST "$BASE_URL/mcp/tools/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "toolName":"rag:search_documents",
    "arguments":{"query":"microservices","topK":3}
  }' | jq

# Test 5: Get document info
echo "5. Getting document info..."
curl -s "$BASE_URL/documents/$DOC_ID" | jq

echo "=== Tests Completed ==="
```

### Integration Tests

```java
// File: backend/rag-mcp-server/src/test/java/de/jivz/rag/RagServiceIntegrationTest.java
@SpringBootTest
@Testcontainers
class RagServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
        .withDatabaseName("rag_test_db")
        .withUsername("test")
        .withPassword("test");
    
    @Autowired
    private RagService ragService;
    
    @Test
    void testDocumentUploadAndSearch() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Spring Boot is a Java framework".getBytes()
        );
        
        // When - Upload
        DocumentDto doc = ragService.uploadDocument(file);
        
        // Then
        assertThat(doc.getId()).isNotNull();
        assertThat(doc.getChunkCount()).isGreaterThan(0);
        
        // When - Search
        List<SearchResultDto> results = ragService.searchDocuments(
            "Java framework",
            5,
            0.7
        );
        
        // Then
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getRelevance()).isGreaterThan(0.7);
    }
}
```

## 🔧 Troubleshooting

### Problem: pgvector extension not found

**Symptom:**
```
ERROR: type "vector" does not exist
```

**Solution:**
```bash
# Connect to database
docker exec -it rag-postgres psql -U postgres -d rag_db

# Create extension
CREATE EXTENSION vector;

# Verify
SELECT * FROM pg_extension WHERE extname = 'vector';
```

### Problem: Embedding generation fails

**Symptom:**
```
Failed to generate embedding: 401 Unauthorized
```

**Solution:**
```bash
# Check API key is set
echo $OPENROUTER_API_KEY

# Test API key
curl -H "Authorization: Bearer $OPENROUTER_API_KEY" \
  https://openrouter.ai/api/v1/models

# If invalid, set correct key
export OPENROUTER_API_KEY='sk-or-v1-correct-key'
```

### Problem: Document parsing fails

**Symptom:**
```
UnsupportedFileFormatException: Cannot parse file type
```

**Solution:**
1. Check supported formats: PDF, EPUB, TXT
2. Verify file is not corrupted:
```bash
file document.pdf  # Should show "PDF document"
```
3. Check Apache Tika dependency in pom.xml

### Problem: Low search relevance

**Symptom:**
All search results have relevance < 0.5

**Solution:**
1. Lower threshold:
```json
{"query": "...", "threshold": 0.5}
```
2. Check embedding quality:
```bash
# Verify embeddings are generated
SELECT COUNT(*) FROM document_chunks WHERE embedding IS NOT NULL;
```
3. Try hybrid search for better results

## 💡 Best Practices

### 1. Document Chunking

✅ **DO:**
- Use 500-1000 tokens per chunk for balanced context
- Add 10-15% overlap to preserve context
- Keep chunk boundaries at sentence endings

❌ **DON'T:**
- Don't create chunks > 2000 tokens (context limit)
- Don't split mid-sentence
- Don't use zero overlap

### 2. Embedding Generation

✅ **DO:**
- Batch embedding requests for efficiency
- Cache embeddings when possible
- Use appropriate embedding model for your language

❌ **DON'T:**
- Don't generate embeddings synchronously for large documents
- Don't exceed API rate limits
- Don't store embeddings without normalization

### 3. Search Optimization

✅ **DO:**
- Use threshold filtering (0.7-0.8 for quality)
- Implement hybrid search for better recall
- Add metadata filtering when possible

❌ **DON'T:**
- Don't return topK > 20 (diminishing returns)
- Don't ignore relevance scores
- Don't search without preprocessing query

## 📚 Related Documentation

- **[OpenRouter Provider Feature](../features/OPENROUTER_PROVIDER_FEATURE.md)** - LLM integration
- **[Full-Text Search Guide](../features/FULL_TEXT_SEARCH_GUIDE.md)** - Keyword search
- **[MCP Multi-Provider Architecture](MCP_MULTI_PROVIDER_ARCHITECTURE.md)** - MCP system
- **[PostgreSQL Memory Setup](../setup/POSTGRESQL_MEMORY_SETUP.md)** - Database configuration

## 🎓 Summary

RAG MCP Server Integration provides:

✅ **Multi-Format Support**: PDF, EPUB, TXT parsing  
✅ **Semantic Search**: pgvector-powered vector similarity  
✅ **Hybrid Search**: Combines semantic + keyword matching  
✅ **MCP Integration**: Seamless OpenRouter Service integration  
✅ **Production Ready**: Chunking, embedding, indexing pipeline  
✅ **Scalable**: PostgreSQL + pgvector for large document collections  

**Quick Reference:**
- Port: **8086**
- Embedding Model: **text-embedding-3-small** (1536 dimensions)
- Chunk Size: **500 tokens** with 50 overlap
- Database: **PostgreSQL 16** with pgvector extension
- Supported Formats: **PDF, EPUB, TXT**
  -H "Content-Type: application/json" \
  -d '{"name": "search_documents", "arguments": {"query": "AI"}}'
```

## 🏗 Архитектура

```
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  perplexity-     │────▶│  rag-mcp-server  │────▶│  OpenRouter API  │
│    service       │     │    (port 8086)   │     │   (embeddings)   │
└──────────────────┘     └────────┬─────────┘     └──────────────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │   PostgreSQL +   │
                         │     pgvector     │
                         └──────────────────┘
```

## 📝 Следующие шаги

1. [ ] Создать базу данных `rag_db` с расширением pgvector
2. [ ] Настроить `OPENROUTER_API_KEY`
3. [ ] Запустить `rag-mcp-server`
4. [ ] Загрузить тестовые документы
5. [ ] Протестировать поиск через perplexity-service

