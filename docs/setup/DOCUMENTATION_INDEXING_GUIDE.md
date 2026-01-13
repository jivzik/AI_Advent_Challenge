# Documentation Indexing Script

## Overview

The `index-project-docs.sh` script automatically indexes all project documentation into the RAG (Retrieval Augmented Generation) system. This enables AI-powered semantic search across all project documentation.

## Features

- **Automatic Discovery**: Finds all `.md` files in `/docs` directory and root folder
- **Batch Processing**: Uploads all documentation files to RAG service
- **Category Classification**: Automatically categorizes documents (quickstarts, architecture, features, setup)
- **Error Handling**: Robust error handling with detailed logging
- **Dry-Run Mode**: Preview files without uploading
- **Force Reindex**: Delete existing documents before reindexing
- **Colored Output**: Visual feedback with color-coded status messages

## Usage

### Basic Usage

```bash
# Index all documentation
./index-project-docs.sh
```

### Options

| Option | Description |
|--------|-------------|
| `--dry-run` | Show list of files without uploading |
| `--force` | Delete existing documents before indexing |
| `-h, --help` | Show help message |

### Examples

```bash
# Preview files to be indexed
./index-project-docs.sh --dry-run

# Force reindex (delete old + upload new)
./index-project-docs.sh --force

# Show help
./index-project-docs.sh --help
```

## Prerequisites

1. **RAG Service Running**: The RAG service must be running on port 8086
   ```bash
   cd backend/rag-mcp-server
   ./mvnw spring-boot:run
   ```

2. **curl Installed**: The script requires `curl` command
   ```bash
   # Install on Ubuntu/Debian
   sudo apt install curl
   
   # Install on macOS
   brew install curl
   ```

## Output Format

The script provides color-coded output:

- 🔵 **[INFO]** - General information (blue)
- 🟢 **[OK]** - Successful operation (green)
- 🔴 **[ERROR]** - Error occurred (red)
- 🟡 **[WARN]** - Warning message (yellow)

### Example Output

```
========================================
Индексация документации проекта в RAG
========================================
[INFO] Проверка доступности RAG сервиса на http://localhost:8086...
[OK] ✓ RAG сервис доступен
[INFO] Поиск файлов документации...
[INFO] Найдено файлов: 44
========================================
Обработка файлов документации
========================================
[OK] ✓ README.md (9633 байт, 12 чанков, категория: project-root)
[OK] ✓ FEATURES_INDEX.md (8377 байт, 8 чанков, категория: project-root)
[OK] ✓ META_PROMPTING_FEATURE.md (9741 байт, 15 чанков, категория: features)
...
========================================
Индексация завершена
========================================
[INFO] Всего файлов найдено: 44
[OK] ✓ Успешно загружено: 44
[INFO] Ошибок: 0
```

## Document Categories

The script automatically assigns categories based on file location:

| Location | Category |
|----------|----------|
| `README.md`, `FEATURES_INDEX.md` | `project-root` |
| `docs/quickstarts/*` | `quickstarts` |
| `docs/architecture/*` | `architecture` |
| `docs/features/*` | `features` |
| `docs/setup/*` | `setup` |
| `docs/development/*` | `development` |
| Other | `documentation` |

## API Endpoints Used

The script interacts with the RAG service via these endpoints:

- **GET** `/api/documents` - List all indexed documents
- **POST** `/api/documents/upload` - Upload a document for indexing
- **DELETE** `/api/documents/{id}` - Delete a document by ID

## Troubleshooting

### RAG Service Not Running

```
[ERROR] ✗ RAG сервис недоступен на http://localhost:8086
[INFO] Запустите RAG сервис командой: cd backend/rag-mcp-server && mvn spring-boot:run
```

**Solution**: Start the RAG service:
```bash
cd backend/rag-mcp-server
./mvnw spring-boot:run
```

### curl Not Found

```
[ERROR] ✗ curl не установлен. Установите: sudo apt install curl
```

**Solution**: Install curl:
```bash
# Ubuntu/Debian
sudo apt install curl

# macOS
brew install curl
```

### Upload Failures

If individual files fail to upload:

1. Check file permissions: `ls -la docs/`
2. Verify file is not corrupted: `cat docs/path/to/file.md`
3. Check RAG service logs for errors
4. Try uploading the specific file manually:
   ```bash
   curl -X POST http://localhost:8086/api/documents/upload \
     -F "file=@docs/path/to/file.md"
   ```

## Integration with RAG System

Once indexed, the documentation can be searched using:

### REST API

```bash
curl -X POST http://localhost:8086/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "How to use OpenRouter?",
    "limit": 5
  }'
```

### Frontend UI

1. Navigate to http://localhost:5173
2. Open "RAG Search" tab
3. Enter search query
4. View relevant documentation chunks

### Chat with AI

The RAG system can be integrated with chat:

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "How do I set up Perplexity integration?",
    "userId": "user-123",
    "useRag": true
  }'
```

## Technical Details

### File Discovery

The script uses `find` command to recursively search for `.md` files:

```bash
find "${SCRIPT_DIR}/docs" -type f -name "*.md" -print0
```

### Upload Process

Each file is uploaded via multipart/form-data:

```bash
curl -X POST "${RAG_UPLOAD_ENDPOINT}" \
  -F "file=@${filepath}"
```

### Error Handling

- HTTP 200: Success ✓
- HTTP 400: Bad request (empty file, invalid format)
- HTTP 500: Server error (processing failed)

## Performance

- **Average Speed**: ~2-3 files per second
- **44 Files**: ~15-20 seconds total
- **Network**: Local (minimal latency)
- **Chunking**: ~10-15 chunks per file (500-1000 tokens each)

## Future Enhancements

Potential improvements:

- [ ] Parallel uploads (batch processing)
- [ ] Progress bar for large documentation sets
- [ ] Watch mode (auto-reindex on file changes)
- [ ] Selective reindexing (only changed files)
- [ ] Configuration file support (.indexrc)
- [ ] Custom metadata injection
- [ ] PDF and other format support

## Related Documentation

- [RAG Integration Guide](architecture/RAG_MCP_INTEGRATION.md)
- [Full-Text Search Guide](features/FULL_TEXT_SEARCH_GUIDE.md)
- [MCP Service Quickstart](quickstarts/MCP_SERVICE_QUICKSTART.md)

---

**Last Updated**: 2026-01-12  
**Script Version**: 1.0.0  
**Compatible with**: RAG MCP Server v1.x

