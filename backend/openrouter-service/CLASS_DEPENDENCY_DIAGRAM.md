# Class Dependency Diagram - Refactored ChatWithToolsService

## Legend
```
┌─────────┐
│  Class  │  = Service/Component
└─────────┘
    │
    ├──→    = Dependency (uses)
    │
    ▼       = Inheritance/Implementation
```

## Complete Dependency Graph

```
┌───────────────────────────────────────────────────────────┐
│              ChatWithToolsService                         │
│              (High-Level Orchestrator)                    │
│                                                           │
│  + chatWithTools(ChatRequest): ChatResponse              │
│  + chatWithTools(String): ChatResponse                   │
│  + getAllConversationIds(): List<String>                 │
│  - saveToHistory(String, String, String): void           │
└───────────────────────────────────────────────────────────┘
         │
         ├──→ MCPFactory
         ├──→ MessageBuilderService
         ├──→ ToolExecutionOrchestrator
         ├──→ ConversationHistoryService
         ├──→ MemoryRepository
         └──→ OpenRouterProperties
         
┌───────────────────────────────────────────────────────────┐
│              MessageBuilderService                        │
│                                                           │
│  + buildMessages(String, String,                         │
│                  List<ToolDefinition>): List<Message>    │
└───────────────────────────────────────────────────────────┘
         │
         ├──→ ContextDetectionService
         ├──→ ConversationHistoryService
         └──→ PromptLoaderService

┌───────────────────────────────────────────────────────────┐
│              ContextDetectionService                      │
│                                                           │
│  + detectContext(String,                                 │
│                  List<ToolDefinition>): String           │
│  - parseContext(String): String                          │
└───────────────────────────────────────────────────────────┘
         │
         ├──→ OpenRouterApiClient
         ├──→ PromptLoaderService
         └──→ ObjectMapper

┌───────────────────────────────────────────────────────────┐
│              OpenRouterApiClient                          │
│                                                           │
│  + sendChatRequest(List<Message>,                        │
│                    Double, Integer): String              │
│  + sendContextDetectionRequest(List<Message>): String    │
└───────────────────────────────────────────────────────────┘
         │
         ├──→ WebClient (openRouterWebClient)
         └──→ OpenRouterProperties

┌───────────────────────────────────────────────────────────┐
│              ToolExecutionOrchestrator                    │
│                                                           │
│  + executeToolLoop(List<Message>, Double): String        │
│  - executeTools(...): void                               │
│  - executeSingleTool(ToolCall): String                   │
│  - formatFinalAnswer(String, Set<String>): String        │
└───────────────────────────────────────────────────────────┘
         │
         ├──→ OpenRouterApiClient
         ├──→ ResponseParsingService
         ├──→ MCPFactory
         ├──→ SourceExtractionService
         └──→ ObjectMapper

┌───────────────────────────────────────────────────────────┐
│              ResponseParsingService                       │
│                                                           │
│  + parseWithRetry(String, List<Message>,                │
│                    Double): ToolResponse                 │
│  - parseWithStrategies(String): ToolResponse             │
└───────────────────────────────────────────────────────────┘
         │
         ├──→ List<ResponseParserStrategy>
         ├──→ OpenRouterApiClient
         └──→ PromptLoaderService

┌───────────────────────────────────────────────────────────┐
│         <<interface>>                                     │
│         ResponseParserStrategy                            │
│                                                           │
│  + canParse(String): boolean                             │
│  + parse(String): ToolResponse                           │
└───────────────────────────────────────────────────────────┘
         ▲
         │
         ├── JsonResponseParser
         └── TextResponseParser

┌───────────────────────────────────────────────────────────┐
│              JsonResponseParser                           │
│              implements ResponseParserStrategy            │
│                                                           │
│  + canParse(String): boolean                             │
│  + parse(String): ToolResponse                           │
│  - cleanJsonResponse(String): String                     │
└───────────────────────────────────────────────────────────┘
         │
         └──→ ObjectMapper

┌───────────────────────────────────────────────────────────┐
│              TextResponseParser                           │
│              implements ResponseParserStrategy            │
│                                                           │
│  + canParse(String): boolean                             │
│  + parse(String): ToolResponse                           │
└───────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────┐
│              SourceExtractionService                      │
│                                                           │
│  + extractSourcesFromRagResult(String,                   │
│                                Set<String>): void         │
│  + appendSources(String, Set<String>): String            │
│  - isValidDocumentName(String): boolean                  │
└───────────────────────────────────────────────────────────┘
         │
         └──→ ObjectMapper
```

## Package Dependencies

```
service/
│
├─ ChatWithToolsService ───┬──→ client/OpenRouterApiClient
│                          ├──→ context/ContextDetectionService
│                          ├──→ message/MessageBuilderService
│                          ├──→ orchestrator/ToolExecutionOrchestrator
│                          └──→ source/SourceExtractionService
│
├─ client/
│  └─ OpenRouterApiClient ─────→ (WebClient, Properties)
│
├─ context/
│  └─ ContextDetectionService ──→ client/OpenRouterApiClient
│
├─ message/
│  └─ MessageBuilderService ────→ context/ContextDetectionService
│
├─ orchestrator/
│  └─ ToolExecutionOrchestrator ┬→ client/OpenRouterApiClient
│                               ├→ parser/ResponseParsingService
│                               └→ source/SourceExtractionService
│
├─ parser/
│  ├─ ResponseParserStrategy ───→ (interface)
│  ├─ JsonResponseParser ───────→ implements ResponseParserStrategy
│  ├─ TextResponseParser ───────→ implements ResponseParserStrategy
│  └─ ResponseParsingService ───┬→ List<ResponseParserStrategy>
│                               └→ client/OpenRouterApiClient
│
└─ source/
   └─ SourceExtractionService ──→ (ObjectMapper)
```

## Dependency Flow (Execution Order)

```
1. User Request
   │
   ├─→ ChatWithToolsService.chatWithTools(request)
   │
2. Build Messages
   ├─→ MessageBuilderService.buildMessages()
   │   │
   │   ├─→ ContextDetectionService.detectContext()
   │   │   │
   │   │   └─→ OpenRouterApiClient.sendContextDetectionRequest()
   │   │
   │   └─→ ConversationHistoryService.getHistory()
   │
3. Execute Tool Loop
   ├─→ ToolExecutionOrchestrator.executeToolLoop()
   │   │
   │   ├─→ OpenRouterApiClient.sendChatRequest()
   │   │
   │   ├─→ ResponseParsingService.parseWithRetry()
   │   │   │
   │   │   ├─→ JsonResponseParser.parse()
   │   │   └─→ TextResponseParser.parse()
   │   │
   │   ├─→ MCPFactory.route() [if tools needed]
   │   │
   │   └─→ SourceExtractionService.extractSourcesFromRagResult()
   │
4. Save History
   ├─→ ConversationHistoryService.addMessage()
   │
5. Return Response
   └─→ ChatResponse
```

## Circular Dependency Check

✅ **No circular dependencies detected!**

All dependencies flow in one direction:
- Top → Down (Orchestrator → Services)
- Left → Right (High-level → Low-level)

## Key Architectural Patterns

1. **Orchestrator Pattern**
   - `ChatWithToolsService` as high-level coordinator

2. **Strategy Pattern**
   - `ResponseParserStrategy` with multiple implementations

3. **Dependency Injection**
   - All services use constructor injection

4. **Single Responsibility**
   - Each service has one clear purpose

5. **Facade Pattern**
   - `OpenRouterApiClient` hides API complexity

## Notes

- All services are Spring-managed beans (`@Service`)
- Constructor injection via `@RequiredArgsConstructor` (Lombok)
- No static dependencies
- Easy to mock for testing
- Clear separation of concerns

