# Analytics Service

Local data analytics service with LLM-powered insights.

## Overview

The Analytics Service analyzes CSV, JSON, and log files to provide AI-powered insights and recommendations. It uses the LLM Chat Service to generate natural language responses based on data analysis.

## Features

- **File Upload**: Support for CSV, JSON, and plain text log files (up to 50MB)
- **Data Analysis**: Automatic detection and analysis based on query type
  - Error analysis (frequency, distribution)
  - Time-based analysis (hourly distribution, peak times)
  - Performance analysis (response times, slowest endpoints)
  - Statistical analysis (min, max, average)
- **LLM Integration**: Uses Ollama via llm-chat-service for insights
- **Structured Response**: Returns answer, raw data, insights, and recommendations

## API Endpoints

### POST /api/analytics/analyze

Analyze a data file with a natural language query.

**Request**:
- Content-Type: `multipart/form-data`
- Parameters:
  - `query` (required): User's question about the data
  - `file` (required): Data file (CSV/JSON/TXT)
  - `includeVisualization` (optional): Boolean
  - `detailedRecommendations` (optional): Boolean

**Response**:
```json
{
  "answer": "NullPointerException is most common (45%)...",
  "rawData": {
    "error_distribution": {
      "NullPointerException": 23,
      "TimeoutException": 15
    }
  },
  "insights": [
    "Most common error: NullPointerException (45%)",
    "Total records analyzed: 51"
  ],
  "recommendations": [
    "Add null check in LlmChatService:78",
    "Increase timeout for specific users"
  ],
  "metadata": {
    "fileFormat": "csv",
    "rowsAnalyzed": 51,
    "processingTimeMs": 2340,
    "timestamp": "2026-01-22T10:30:00"
  }
}
```

### GET /api/analytics/health

Health check endpoint.

## File Formats

### CSV
```csv
timestamp,service,level,message,error_type
2026-01-22 10:15:23,llm-chat-service,ERROR,chat() failed,NullPointerException
```

### JSON
```json
[
  {
    "timestamp": "2026-01-22T10:00:00",
    "endpoint": "/api/chat",
    "status": 200,
    "response_time_ms": 2340
  }
]
```

### Plain Logs
```
2026-01-22 10:15:23 ERROR NullPointerException in chat() method
2026-01-22 10:18:45 ERROR TimeoutException calling Ollama
```

## Configuration

Application configuration in `application.yml`:

```yaml
server:
  port: 8090

llm:
  chat:
    service:
      url: http://localhost:8084
      timeout: 60

spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```

## Dependencies

- **llm-chat-service** (port 8084): For LLM-powered insights
- **Ollama** (port 11434): For local LLM inference

## Running the Service

### Prerequisites

1. Java 21 or higher
2. Maven 3.8+
3. llm-chat-service running on port 8084
4. Ollama running on port 11434

### Build and Run

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

The service will start on port 8090.

## Example Usage

```bash
# Analyze error logs
curl -X POST http://localhost:8090/api/analytics/analyze \
  -F "query=What errors occur most frequently?" \
  -F "file=@service.log"

# Analyze API metrics
curl -X POST http://localhost:8090/api/analytics/analyze \
  -F "query=Which endpoint is slowest?" \
  -F "file=@api_metrics.csv" \
  -F "detailedRecommendations=true"
```

## Testing

```bash
# Run unit tests
mvn test

# Run with coverage
mvn clean test jacoco:report
```

## Architecture

```
AnalyticsController
  ↓
AnalyticsService
  ├─ FileParser (CSV/JSON/Log)
  ├─ DataAnalyzer
  ├─ PromptBuilder
  └─ LlmChatClient → llm-chat-service → Ollama
```

## Error Handling

- **Empty query**: "Please enter a question"
- **No file**: "Please select a file"
- **Unsupported format**: "Only CSV, JSON, TXT supported"
- **File too large**: "File must be < 50MB"
- **Parse failure**: "Could not parse file"
- **LLM unavailable**: "LLM service unavailable"

## Performance

- Files < 1000 rows: < 5 seconds
- Files 1000-10000 rows: < 30 seconds
- Maximum file size: 50MB

## Platform Context

The LLM is provided with context about our platform:
- 9 microservices on ports 8080-8089
- Common services: llm-chat-service (8084), analytics-service (8090)
- Common errors: NullPointerException, TimeoutException
- Architecture pattern: Controller → Service → Client

This ensures recommendations are specific and actionable for our codebase.
