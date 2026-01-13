# Developer Code Style Guide - AI Advent Challenge

Это руководство по стилю кода проекта. Используй его при генерации примеров кода для разработчиков.

---

## 🎯 Общие принципы:

1. **Следуй conventions проекта** - не изобретай новые стили
2. **Используй существующие паттерны** - смотри на реальный код проекта
3. **Полные примеры** - не сниппеты из 2 строк, а рабочий код
4. **Комментарии** - объясняй неочевидные моменты
5. **Consistency** - стиль должен быть единообразным

---

## ☕ Java / Spring Boot Style

### Package Structure:
```
com.example.{service-name}/
├── controller/          # REST API endpoints (@RestController)
├── service/            # Business logic (@Service)
├── repository/         # Data access (@Repository)
├── model/              # DTOs, Entities (@Entity)
├── config/             # Configuration (@Configuration)
├── client/             # External API clients
├── provider/           # MCP Providers (for mcp-service)
└── exception/          # Custom exceptions
```

### Class Naming:
```java
// Controllers
ChatController.java           // Handles /api/chat
DevAssistantController.java   // Handles /api/dev

// Services
ChatService.java              // Business logic for chat
DevAssistantService.java      // Business logic for dev assistant

// Repositories
ConversationRepository.java   // Data access for conversations
DocumentRepository.java       // Data access for documents

// Models
ChatRequest.java              // Request DTO
ChatResponse.java             // Response DTO
Conversation.java             // Entity class

// Providers (MCP)
NativeToolProvider.java       // MCP tool provider
GitToolProvider.java          // Git operations provider
```

### Annotations - обязательно используй:

```java
// Lombok (ALWAYS USE)
@Slf4j                        // Logging
@Data                         // Getters/Setters/toString/equals/hashCode
@Builder                      // Builder pattern
@NoArgsConstructor            // Default constructor
@AllArgsConstructor           // All-args constructor
@RequiredArgsConstructor      // Required fields constructor

// Spring
@RestController               // REST controller
@Service                      // Service layer
@Repository                   // Repository layer
@Component                    // Generic Spring component
@Configuration                // Configuration class
@Autowired                    // Dependency injection (or use constructor injection)

// JPA
@Entity                       // Database entity
@Table(name = "...")          // Custom table name
@Id                           // Primary key
@GeneratedValue               // Auto-generated ID
@Column(name = "...")         // Custom column name

// Validation
@Valid                        // Enable validation
@NotNull                      // Not null constraint
@NotBlank                     // Not blank constraint
@Size(min = ..., max = ...)   // Size constraint
```

### Constructor Injection (PREFERRED):

```java
// ✅ ПРАВИЛЬНО: Constructor Injection
@Service
@Slf4j
public class ChatService {
    
    private final ChatRepository chatRepository;
    private final OpenRouterClient openRouterClient;
    
    // @Autowired опционально для single constructor
    public ChatService(
            ChatRepository chatRepository,
            OpenRouterClient openRouterClient) {
        this.chatRepository = chatRepository;
        this.openRouterClient = openRouterClient;
    }
}

// ❌ НЕПРАВИЛЬНО: Field Injection (избегай)
@Service
@Slf4j
public class ChatService {
    @Autowired
    private ChatRepository chatRepository; // Field injection - bad practice!
}
```

### Builder Pattern (Lombok):

```java
// ✅ ИСПОЛЬЗУЙ BUILDER для DTOs
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String message;
    private String userId;
    private String conversationId;
    private Double temperature;
    private Boolean jsonMode;
}

// Usage:
ChatRequest request = ChatRequest.builder()
    .message("Hello")
    .userId("user-123")
    .temperature(0.7)
    .build();
```

### Logging:

```java
// ✅ ПРАВИЛЬНО: Slf4j with Lombok
@Slf4j
@Service
public class MyService {
    
    public void processRequest(String userId) {
        log.info("Processing request for user: {}", userId);
        
        try {
            // Business logic
            log.debug("Request details: {}", requestData);
        } catch (Exception e) {
            log.error("Failed to process request for user: {}", userId, e);
            throw e;
        }
    }
}

// Log levels:
log.trace("Very detailed debug info");
log.debug("Debug information");
log.info("Important business events");
log.warn("Warning messages");
log.error("Error messages with exception", exception);
```

### REST Controllers:

```java
@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {
    
    private final ChatService chatService;
    
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request from user: {}", request.getUserId());
        
        try {
            ChatResponse response = chatService.processChat(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to process chat request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Conversation> getConversation(@PathVariable String id) {
        return chatService.getConversation(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

### Service Layer:

```java
@Service
@Slf4j
public class ChatService {
    
    private final ConversationRepository conversationRepository;
    private final OpenRouterClient openRouterClient;
    
    public ChatService(
            ConversationRepository conversationRepository,
            OpenRouterClient openRouterClient) {
        this.conversationRepository = conversationRepository;
        this.openRouterClient = openRouterClient;
    }
    
    public ChatResponse processChat(ChatRequest request) {
        log.debug("Processing chat for user: {}", request.getUserId());
        
        // Validate input
        validateRequest(request);
        
        // Get or create conversation
        Conversation conversation = getOrCreateConversation(
            request.getUserId(), 
            request.getConversationId()
        );
        
        // Call AI service
        String aiResponse = openRouterClient.chat(
            request.getMessage(), 
            conversation.getHistory()
        );
        
        // Save to database
        conversation.addMessage(request.getMessage(), aiResponse);
        conversationRepository.save(conversation);
        
        // Build response
        return ChatResponse.builder()
            .response(aiResponse)
            .conversationId(conversation.getId())
            .build();
    }
    
    private void validateRequest(ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
    }
    
    private Conversation getOrCreateConversation(String userId, String conversationId) {
        if (conversationId != null) {
            return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        }
        return Conversation.builder()
            .userId(userId)
            .createdAt(Instant.now())
            .build();
    }
}
```

### Repository:

```java
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    
    // Spring Data JPA auto-implements these
    List<Conversation> findByUserId(String userId);
    
    Optional<Conversation> findByUserIdAndId(String userId, String conversationId);
    
    @Query("SELECT c FROM Conversation c WHERE c.userId = :userId AND c.createdAt > :since")
    List<Conversation> findRecentConversations(
        @Param("userId") String userId,
        @Param("since") Instant since
    );
}
```

### Configuration:

```java
@Configuration
@ConfigurationProperties(prefix = "openrouter")
@Data
public class OpenRouterConfig {
    private String apiKey;
    private String baseUrl = "https://openrouter.ai/api/v1";
    private Integer timeout = 30000;
    private String defaultModel = "anthropic/claude-3.5-sonnet";
}

// application.properties:
// openrouter.api-key=${OPENROUTER_API_KEY}
// openrouter.base-url=https://openrouter.ai/api/v1
// openrouter.timeout=30000
```

### WebClient (Reactive HTTP):

```java
@Component
@Slf4j
public class OpenRouterClient {
    
    private final WebClient webClient;
    
    public OpenRouterClient(
            OpenRouterConfig config,
            WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .baseUrl(config.getBaseUrl())
            .defaultHeader("Authorization", "Bearer " + config.getApiKey())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
    
    public Mono<ChatResponse> chatAsync(ChatRequest request) {
        return webClient.post()
            .uri("/chat/completions")
            .bodyValue(request)
            .retrieve()
            .onStatus(HttpStatus::isError, response -> {
                log.error("API error: {}", response.statusCode());
                return Mono.error(new RuntimeException("API call failed"));
            })
            .bodyToMono(ChatResponse.class)
            .doOnSuccess(response -> log.debug("Received response: {}", response))
            .doOnError(error -> log.error("Request failed", error));
    }
}
```

### Exception Handling:

```java
// Custom Exception
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// Global Exception Handler
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.builder()
                .error("Resource not found")
                .message(ex.getMessage())
                .build());
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
            .body(ErrorResponse.builder()
                .error("Invalid request")
                .message(ex.getMessage())
                .build());
    }
}
```

---

## 🎨 TypeScript / Vue 3 Style

### Project Structure:
```
frontend/src/
├── components/         # Vue components
│   ├── ChatInterface.vue
│   ├── DevAssistantView.vue
│   └── ...
├── api/               # API clients
│   ├── chat.ts
│   └── dev-assistant.ts
├── types/             # TypeScript types
│   ├── chat.ts
│   └── common.ts
├── composables/       # Composition API reusable logic
│   ├── useChat.ts
│   └── useDevAssistant.ts
├── stores/            # Pinia stores (state management)
│   └── chatStore.ts
└── utils/             # Utility functions
    └── format.ts
```

### Component Naming:
```
ChatInterface.vue           # Main chat component
DevAssistantView.vue        # Dev assistant view
MessageItem.vue             # Message display component
ToolsSidebar.vue            # Tools sidebar
```

### Vue 3 Composition API (ALWAYS USE):

```vue
<template>
  <div class="chat-interface">
    <div class="messages">
      <MessageItem
        v-for="message in messages"
        :key="message.id"
        :message="message"
      />
    </div>
    
    <input
      v-model="inputMessage"
      @keyup.enter="sendMessage"
      placeholder="Type a message..."
    />
    
    <button
      @click="sendMessage"
      :disabled="isLoading"
    >
      {{ isLoading ? 'Sending...' : 'Send' }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useChatStore } from '@/stores/chatStore'
import MessageItem from './MessageItem.vue'
import type { Message } from '@/types/chat'

// Props (if needed)
interface Props {
  conversationId?: string
}
const props = defineProps<Props>()

// Emits (if needed)
interface Emits {
  (e: 'message-sent', message: Message): void
}
const emit = defineEmits<Emits>()

// Store
const chatStore = useChatStore()

// Reactive state
const messages = ref<Message[]>([])
const inputMessage = ref('')
const isLoading = ref(false)

// Computed
const hasMessages = computed(() => messages.value.length > 0)

// Methods
async function sendMessage() {
  if (!inputMessage.value.trim()) return
  
  isLoading.value = true
  try {
    const response = await chatStore.sendMessage({
      message: inputMessage.value,
      conversationId: props.conversationId
    })
    
    messages.value.push(response)
    emit('message-sent', response)
    inputMessage.value = ''
  } catch (error) {
    console.error('Failed to send message:', error)
    alert('Failed to send message')
  } finally {
    isLoading.value = false
  }
}

// Lifecycle
onMounted(async () => {
  if (props.conversationId) {
    messages.value = await chatStore.loadMessages(props.conversationId)
  }
})
</script>

<style scoped lang="scss">
.chat-interface {
  display: flex;
  flex-direction: column;
  height: 100%;
  
  .messages {
    flex: 1;
    overflow-y: auto;
    padding: 1rem;
  }
  
  input {
    padding: 0.75rem;
    border: 1px solid #ccc;
    border-radius: 4px;
    font-size: 1rem;
  }
  
  button {
    padding: 0.75rem 1.5rem;
    background: #007bff;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    
    &:hover:not(:disabled) {
      background: #0056b3;
    }
    
    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }
}
</style>
```

### TypeScript Types:

```typescript
// types/chat.ts

export interface Message {
  id: string
  content: string
  role: 'user' | 'assistant' | 'system'
  timestamp: Date
  conversationId: string
}

export interface ChatRequest {
  message: string
  userId: string
  conversationId?: string
  temperature?: number
  jsonMode?: boolean
  systemPrompt?: string
}

export interface ChatResponse {
  response: string
  conversationId: string
  model?: string
  usage?: {
    promptTokens: number
    completionTokens: number
    totalTokens: number
  }
}

export interface Conversation {
  id: string
  userId: string
  title: string
  createdAt: Date
  updatedAt: Date
  messages: Message[]
}
```

### API Client:

```typescript
// api/chat.ts

import type { ChatRequest, ChatResponse, Conversation } from '@/types/chat'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export async function sendChatMessage(request: ChatRequest): Promise<ChatResponse> {
  const response = await fetch(`${API_BASE_URL}/api/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  })
  
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }
  
  return await response.json()
}

export async function getConversation(conversationId: string): Promise<Conversation> {
  const response = await fetch(`${API_BASE_URL}/api/chat/${conversationId}`)
  
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }
  
  return await response.json()
}

export async function listConversations(userId: string): Promise<Conversation[]> {
  const response = await fetch(`${API_BASE_URL}/api/conversations?userId=${userId}`)
  
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }
  
  return await response.json()
}
```

### Composable (Reusable Logic):

```typescript
// composables/useChat.ts

import { ref, computed } from 'vue'
import { sendChatMessage } from '@/api/chat'
import type { Message, ChatRequest } from '@/types/chat'

export function useChat(conversationId?: string) {
  const messages = ref<Message[]>([])
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  
  const hasMessages = computed(() => messages.value.length > 0)
  
  async function sendMessage(content: string, userId: string) {
    if (!content.trim()) return
    
    isLoading.value = true
    error.value = null
    
    // Add user message immediately
    const userMessage: Message = {
      id: Date.now().toString(),
      content,
      role: 'user',
      timestamp: new Date(),
      conversationId: conversationId || ''
    }
    messages.value.push(userMessage)
    
    try {
      const request: ChatRequest = {
        message: content,
        userId,
        conversationId
      }
      
      const response = await sendChatMessage(request)
      
      // Add assistant response
      const assistantMessage: Message = {
        id: (Date.now() + 1).toString(),
        content: response.response,
        role: 'assistant',
        timestamp: new Date(),
        conversationId: response.conversationId
      }
      messages.value.push(assistantMessage)
      
      return response
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to send message'
      throw err
    } finally {
      isLoading.value = false
    }
  }
  
  function clearMessages() {
    messages.value = []
  }
  
  return {
    messages,
    isLoading,
    error,
    hasMessages,
    sendMessage,
    clearMessages
  }
}
```

### Pinia Store:

```typescript
// stores/chatStore.ts

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Conversation, Message } from '@/types/chat'
import * as chatApi from '@/api/chat'

export const useChatStore = defineStore('chat', () => {
  // State
  const conversations = ref<Conversation[]>([])
  const currentConversationId = ref<string | null>(null)
  const isLoading = ref(false)
  
  // Getters
  const currentConversation = computed(() => 
    conversations.value.find(c => c.id === currentConversationId.value)
  )
  
  // Actions
  async function loadConversations(userId: string) {
    isLoading.value = true
    try {
      conversations.value = await chatApi.listConversations(userId)
    } catch (error) {
      console.error('Failed to load conversations:', error)
      throw error
    } finally {
      isLoading.value = false
    }
  }
  
  async function sendMessage(request: chatApi.ChatRequest) {
    isLoading.value = true
    try {
      const response = await chatApi.sendChatMessage(request)
      
      // Update conversation in store
      const conversation = conversations.value.find(c => c.id === response.conversationId)
      if (conversation) {
        // Add messages to existing conversation
        // ... update logic
      }
      
      return response
    } catch (error) {
      console.error('Failed to send message:', error)
      throw error
    } finally {
      isLoading.value = false
    }
  }
  
  function setCurrentConversation(conversationId: string) {
    currentConversationId.value = conversationId
  }
  
  return {
    // State
    conversations,
    currentConversationId,
    isLoading,
    // Getters
    currentConversation,
    // Actions
    loadConversations,
    sendMessage,
    setCurrentConversation
  }
})
```

---

## 🐚 Bash Scripts Style

### Shebang and Options:

```bash
#!/bin/bash

# Strict mode
set -e          # Exit on error
set -u          # Exit on undefined variable
set -o pipefail # Exit on pipe failure

# Optional for debugging
# set -x        # Print commands before execution
```

### Colors and Output:

```bash
# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[⚠]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗]${NC} $1" >&2
}

# Usage:
log_info "Starting script..."
log_success "Operation completed!"
log_warning "This might take a while"
log_error "Something went wrong"
```

### Script Template:

```bash
#!/bin/bash
set -e

# Script description
# Usage: ./script.sh [options]

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

check_requirements() {
    log_info "Checking requirements..."
    
    if ! command -v java &> /dev/null; then
        log_error "Java not found. Please install Java 21+"
        exit 1
    fi
    
    log_info "All requirements met"
}

main() {
    log_info "Starting script execution..."
    
    check_requirements
    
    # Main logic here
    
    log_info "Script completed successfully!"
}

# Run main function
main "$@"
```

---

## 📝 Comments Style:

### Java Comments:

```java
/**
 * Service for handling chat operations.
 * 
 * @author AI Advent Challenge Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class ChatService {
    
    /**
     * Process a chat request and return AI response.
     * 
     * @param request The chat request containing user message
     * @return ChatResponse with AI-generated response
     * @throws IllegalArgumentException if request is invalid
     */
    public ChatResponse processChat(ChatRequest request) {
        // Validate input
        validateRequest(request);
        
        // TODO: Add rate limiting
        
        // Process message
        return generateResponse(request);
    }
    
    // Private helper method (single-line comment)
    private void validateRequest(ChatRequest request) {
        // Check message is not empty
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
    }
}
```

### TypeScript Comments:

```typescript
/**
 * Chat composable for managing chat state and operations
 * @param conversationId Optional conversation ID to load
 * @returns Chat state and methods
 */
export function useChat(conversationId?: string) {
  // Reactive state
  const messages = ref<Message[]>([])
  const isLoading = ref(false)
  
  /**
   * Send a message to the chat API
   * @param content Message content
   * @param userId User ID
   * @returns Chat response from API
   */
  async function sendMessage(content: string, userId: string) {
    // TODO: Add input validation
    
    // Implementation...
  }
  
  return {
    messages,
    isLoading,
    sendMessage
  }
}
```

---

## 🎨 Naming Conventions Summary:

### Java:
- **Classes:** PascalCase (`ChatService`, `OpenRouterClient`)
- **Methods:** camelCase (`processChat()`, `validateRequest()`)
- **Variables:** camelCase (`userId`, `conversationId`)
- **Constants:** UPPER_SNAKE_CASE (`MAX_RETRIES`, `API_TIMEOUT`)
- **Packages:** lowercase (`com.example.openrouter.service`)

### TypeScript/Vue:
- **Components:** PascalCase (`ChatInterface.vue`, `MessageItem.vue`)
- **Functions:** camelCase (`sendMessage()`, `formatDate()`)
- **Variables:** camelCase (`userId`, `isLoading`)
- **Constants:** UPPER_SNAKE_CASE (`API_BASE_URL`)
- **Types/Interfaces:** PascalCase (`ChatRequest`, `Message`)
- **Files:** kebab-case (`chat-service.ts`, `dev-assistant.vue`)

### Bash:
- **Variables:** UPPER_SNAKE_CASE (`PROJECT_ROOT`, `API_KEY`)
- **Functions:** snake_case (`log_info`, `check_requirements`)
- **Files:** kebab-case (`start-backend.sh`, `test-all.sh`)

---

## ✅ Checklist при генерации кода:

- [ ] Используются правильные аннотации (@Slf4j, @Builder, etc.)
- [ ] Логирование добавлено для важных операций
- [ ] Обработка ошибок (try-catch)
- [ ] Валидация входных данных
- [ ] Комментарии для неочевидной логики
- [ ] Follows project naming conventions
- [ ] Consistent formatting (spaces, indentation)
- [ ] No hardcoded values (use configuration)
- [ ] Full example (not just snippet)

---

## 🎯 Помни:

Код должен быть:
1. **Читаемым** - понятен другим разработчикам
2. **Поддерживаемым** - легко изменять
3. **Тестируемым** - можно покрыть тестами
4. **Документированным** - есть комментарии где нужно
5. **Последовательным** - следует стилю проекта

Не просто генерируй код - создавай **production-ready** решения!