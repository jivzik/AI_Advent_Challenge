# 🎨 Design Guidelines - AI Advent Challenge

**Version:** 1.0.0  
**Letzte Aktualisierung:** 2026-01-17

Dieses Dokument beschreibt die Design-Richtlinien, Coding-Standards und Best Practices für das AI Advent Challenge Projekt.

---

## 📚 Inhaltsverzeichnis

1. [Allgemeine Prinzipien](#allgemeine-prinzipien)
2. [Backend (Java/Spring Boot)](#backend-javaspring-boot)
3. [Frontend (Vue 3/TypeScript)](#frontend-vue-3typescript)
4. [UI/UX Design System](#uiux-design-system)
5. [Naming Conventions](#naming-conventions)
6. [Code-Qualität](#code-qualität)

---

## 🎯 Allgemeine Prinzipien

### Code-Philosophie

1. **Folge den Projekt-Konventionen** - Erfinde keine neuen Styles
2. **Verwende existierende Patterns** - Schaue auf echten Projekt-Code
3. **Vollständige Beispiele** - Keine 2-Zeilen-Snippets, sondern funktionsfähiger Code
4. **Kommentare** - Erkläre unklare Logik
5. **Konsistenz** - Einheitlicher Stil im gesamten Projekt

### Design-Prinzipien

- **Clean & Professional** - Aufgeräumtes, professionelles Erscheinungsbild
- **Responsive** - Funktioniert auf Desktop und Tablet
- **Accessible** - Gute Lesbarkeit und Kontraste
- **Performant** - Schnelle Ladezeiten, flüssige Animationen
- **User-Centric** - Intuitive Benutzerführung

---

## ☕ Backend (Java/Spring Boot)

### Package-Struktur

```
com.example.{service-name}/
├── controller/          # REST API endpoints (@RestController)
├── service/            # Business logic (@Service)
├── repository/         # Data access (@Repository)
├── model/              # DTOs, Entities (@Entity)
├── config/             # Configuration (@Configuration)
├── client/             # External API clients
├── provider/           # MCP Providers
└── exception/          # Custom exceptions
```

### Naming Conventions

| Element | Convention | Beispiel |
|---------|-----------|----------|
| **Klassen** | PascalCase | `ChatService`, `OpenRouterClient` |
| **Methoden** | camelCase | `processChat()`, `validateRequest()` |
| **Variablen** | camelCase | `userId`, `conversationId` |
| **Konstanten** | UPPER_SNAKE_CASE | `MAX_RETRIES`, `API_TIMEOUT` |
| **Packages** | lowercase | `com.example.openrouter.service` |

### Wichtige Lombok-Annotationen

**Immer verwenden:**
```java
@Slf4j                        // Logging
@Data                         // Getters/Setters/toString/equals/hashCode
@Builder                      // Builder pattern
@NoArgsConstructor            // Default constructor
@AllArgsConstructor           // All-args constructor
@RequiredArgsConstructor      // Required fields constructor
```

### Constructor Injection (BEVORZUGT)

```java
@Service
@Slf4j
public class ChatService {
    
    private final ChatRepository chatRepository;
    private final OpenRouterClient openRouterClient;
    
    // Constructor Injection - Best Practice
    public ChatService(
            ChatRepository chatRepository,
            OpenRouterClient openRouterClient) {
        this.chatRepository = chatRepository;
        this.openRouterClient = openRouterClient;
    }
}
```

❌ **Vermeide Field Injection:**
```java
@Autowired
private ChatRepository chatRepository; // BAD PRACTICE!
```

### Logging Best Practices

```java
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
```

**Log Levels:**
- `log.trace()` - Sehr detaillierte Debug-Info
- `log.debug()` - Debug-Informationen
- `log.info()` - Wichtige Business-Events
- `log.warn()` - Warnungen
- `log.error()` - Fehler mit Exception

---

## 🎨 Frontend (Vue 3/TypeScript)

### Project-Struktur

```
frontend/src/
├── components/         # Vue components
├── api/               # API clients
├── types/             # TypeScript types
├── composables/       # Composition API reusable logic
├── stores/            # Pinia stores (state management)
├── styles/            # SCSS styles
└── utils/             # Utility functions
```

### Naming Conventions

| Element | Convention | Beispiel |
|---------|-----------|----------|
| **Components** | PascalCase | `ChatInterface.vue`, `MessageItem.vue` |
| **Funktionen** | camelCase | `sendMessage()`, `formatDate()` |
| **Variablen** | camelCase | `userId`, `isLoading` |
| **Konstanten** | UPPER_SNAKE_CASE | `API_BASE_URL` |
| **Types/Interfaces** | PascalCase | `ChatRequest`, `Message` |
| **Files** | kebab-case | `chat-service.ts`, `dev-assistant.vue` |

### Vue 3 Composition API (IMMER VERWENDEN)

```vue
<template>
  <div class="chat-interface">
    <MessageItem
      v-for="message in messages"
      :key="message.id"
      :message="message"
    />
    
    <input
      v-model="inputMessage"
      @keyup.enter="sendMessage"
      placeholder="Type a message..."
    />
    
    <button @click="sendMessage" :disabled="isLoading">
      {{ isLoading ? 'Sending...' : 'Send' }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import MessageItem from './MessageItem.vue'
import type { Message } from '@/types/chat'

// Props
interface Props {
  conversationId?: string
}
const props = defineProps<Props>()

// Emits
interface Emits {
  (e: 'message-sent', message: Message): void
}
const emit = defineEmits<Emits>()

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
    // Implementation...
  } finally {
    isLoading.value = false
  }
}

// Lifecycle
onMounted(() => {
  // Initialization
})
</script>

<style scoped lang="scss">
@use '@/styles/variables' as *;
@use '@/styles/mixins' as *;

.chat-interface {
  @include flex-column;
  height: 100%;
}
</style>
```

### TypeScript Types

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
}

export interface ChatResponse {
  response: string
  conversationId: string
  model?: string
  usage?: TokenUsage
}
```

---

## 🎨 UI/UX Design System

### Farbpalette

#### Primärfarben
```scss
$primary-gradient: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
$primary-color: #667eea;
$primary-dark: #764ba2;
$secondary-color: #007bff;
```

#### Textfarben
```scss
$text-dark: #2c3e50;
$text-muted: #666;
$text-light: #888;
$text-white: white;
```

#### Hintergrundfarben
```scss
$bg-light: #f5f5f5;
$bg-white: white;
$bg-dark: #1e1e1e;
$bg-dark-secondary: #2d2d2d;
```

#### Status-Farben
```scss
$success-color: #4caf50;   // Grün
$error-bg: #fee;           // Rot
$warning-color: #ffc107;   // Gelb/Orange
$info-color: #2196f3;      // Blau
```

### Schatten

```scss
$shadow-sm: 0 2px 5px rgba(0, 0, 0, 0.1);
$shadow-md: 0 4px 12px rgba(0, 0, 0, 0.15);
$shadow-lg: 0 4px 24px rgba(0, 0, 0, 0.08);
$shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
$shadow-primary: 0 2px 8px rgba(102, 126, 234, 0.3);
$shadow-primary-hover: 0 4px 12px rgba(102, 126, 234, 0.4);
```

### Border Radius

```scss
$radius-sm: 0.25rem;    // 4px
$radius-md: 0.5rem;     // 8px
$radius-lg: 1rem;       // 16px
$radius-xl: 1.5rem;     // 24px
$radius-full: 2rem;     // 32px (für Buttons)
```

### Spacing

```scss
$spacing-xs: 0.25rem;   // 4px
$spacing-sm: 0.5rem;    // 8px
$spacing-md: 1rem;      // 16px
$spacing-lg: 1.5rem;    // 24px
$spacing-xl: 2rem;      // 32px
```

### Typography

```scss
// Font Families
$font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
$font-mono: 'Courier New', Consolas, Monaco, monospace;

// Font Sizes
$font-size-xs: 0.7rem;    // 11.2px
$font-size-sm: 0.75rem;   // 12px
$font-size-base: 0.85rem; // 13.6px
$font-size-md: 0.9rem;    // 14.4px
$font-size-lg: 1rem;      // 16px
$font-size-xl: 1.8rem;    // 28.8px
$font-size-xxl: 2rem;     // 32px
```

### Transitions

```scss
$transition-fast: 0.2s;
$transition-base: 0.3s;
$transition-slow: 0.5s;
```

### SCSS Mixins

#### Flexbox Helpers
```scss
@mixin flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

@mixin flex-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

@mixin flex-column {
  display: flex;
  flex-direction: column;
}
```

#### Button Styles
```scss
@mixin button-primary {
  background: $primary-gradient;
  color: $text-white;
  border: none;
  border-radius: $radius-full;
  padding: 0.875rem 2rem;
  font-size: $font-size-lg;
  font-weight: 600;
  cursor: pointer;
  box-shadow: $shadow-primary;
  transition: all $transition-base;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: $shadow-primary-hover;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}
```

### Component Guidelines

#### Chat Interface Layout

```
┌─────────────────────────────────────┐
│  Header with Gradient            [⚙️] │
├─────────────────────────────────────┤
│  Quick Actions / Suggested Queries  │
├─────────────────────────────────────┤
│                                     │
│  [User message]          👤         │
│                                     │
│  🤖  [AI response]                  │
│      Metadata badges                │
│                                     │
├─────────────────────────────────────┤
│  [Input field...]           [Send]  │
└─────────────────────────────────────┘
```

#### Message Design

**User Messages:**
- Rechts ausgerichtet
- Blauer Hintergrund (`$primary-color`)
- Weiße Schrift
- Abgerundete Ecken
- Schatten: `$shadow-sm`

**AI Messages:**
- Links ausgerichtet
- Grauer Hintergrund (`$bg-light`)
- Dunkle Schrift (`$text-dark`)
- Markdown-Support
- Metadata-Badges unterhalb

#### Badges & Tags

```scss
.badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.75rem;
  border-radius: $radius-md;
  font-size: $font-size-sm;
  font-weight: 600;
  
  &.badge-success {
    background: rgba($success-color, 0.1);
    color: darken($success-color, 10%);
  }
  
  &.badge-info {
    background: rgba($info-color, 0.1);
    color: darken($info-color, 10%);
  }
}
```

### Loading States

- **Typing Indicator:** 3 animierte Punkte
- **Button:** Disabled state + "Loading..." Text
- **Spinner:** Rotate animation, primäre Farbe
- **Skeleton:** Shimmer effect für Content-Placeholder

### Animation Guidelines

```scss
// Fade In
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

// Slide Up
@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// Usage
.message {
  animation: slideUp 0.3s ease-out;
}
```

---

## 📝 Naming Conventions

### Java/Spring Boot

| Element | Pattern | Beispiel |
|---------|---------|----------|
| Controller | `{Feature}Controller` | `ChatController` |
| Service | `{Feature}Service` | `ChatService` |
| Repository | `{Entity}Repository` | `ConversationRepository` |
| Model/DTO | `{Feature}Request/Response` | `ChatRequest`, `ChatResponse` |
| Entity | `{DomainObject}` | `Conversation`, `Message` |
| Config | `{Feature}Config` | `OpenRouterConfig` |

### Vue/TypeScript

| Element | Pattern | Beispiel |
|---------|---------|----------|
| Component | `{Feature}[Type].vue` | `ChatInterface.vue`, `MessageItem.vue` |
| Composable | `use{Feature}.ts` | `useChat.ts`, `useDevAssistant.ts` |
| Store | `{feature}Store.ts` | `chatStore.ts` |
| API Client | `{feature}.ts` | `chat.ts`, `dev-assistant.ts` |
| Type | `{Feature}Type` | `Message`, `ChatRequest` |

---

## ✅ Code-Qualität

### Checkliste für neuen Code

- [ ] Korrekte Annotationen verwendet (@Slf4j, @Builder, etc.)
- [ ] Logging für wichtige Operationen hinzugefügt
- [ ] Error Handling implementiert (try-catch)
- [ ] Input-Validierung vorhanden
- [ ] Kommentare für unklare Logik
- [ ] Folgt Naming Conventions des Projekts
- [ ] Konsistente Formatierung (Spaces, Indentation)
- [ ] Keine hardcoded Values (nutze Configuration)
- [ ] Vollständiges Beispiel (kein Snippet)

### Code muss sein:

1. **Lesbar** - Verständlich für andere Entwickler
2. **Wartbar** - Einfach zu ändern und zu erweitern
3. **Testbar** - Kann mit Unit Tests abgedeckt werden
4. **Dokumentiert** - Kommentare wo nötig
5. **Konsistent** - Folgt dem Projekt-Stil

---

## 🔗 Weitere Ressourcen

- **Backend Code Style:** `/backend/openrouter-service/src/main/resources/prompts/developer-code-style.md`
- **Frontend Prompt:** `/docs/development/Frontend_prompt.md`
- **SCSS Variables:** `/frontend/src/styles/_variables.scss`
- **SCSS Mixins:** `/frontend/src/styles/_mixins.scss`
- **Architecture Docs:** `/docs/architecture/`

---

## 📄 Lizenz & Kontakt

**Projekt:** AI Advent Challenge  
**Version:** 1.0.0  
**Erstellt:** 2026-01-17

Bei Fragen zu den Design-Richtlinien oder Coding-Standards, bitte ein Issue im Repository erstellen.

