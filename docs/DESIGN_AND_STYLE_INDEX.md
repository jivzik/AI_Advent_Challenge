# 🎨 Design & Style Guide Index

Zentrale Übersicht aller Design- und Coding-Richtlinien im AI Advent Challenge Projekt.

---

## 📚 Hauptdokumentation

### 🎯 [DESIGN_GUIDELINES.md](../DESIGN_GUIDELINES.md)
**Vollständiges Design-Handbuch**

- ☕ **Backend (Java/Spring Boot)**
  - Package-Struktur
  - Lombok-Annotationen
  - Constructor Injection vs. Field Injection
  - Logging Best Practices
  - REST Controller Patterns
  - Service Layer Guidelines
  - Repository Patterns
  - Exception Handling

- 🎨 **Frontend (Vue 3/TypeScript)**
  - Project-Struktur
  - Composition API Patterns
  - TypeScript Types & Interfaces
  - Props & Emits
  - Composables (Reusable Logic)
  - Pinia Stores
  - API Client Patterns

- 🎨 **UI/UX Design System**
  - Farbpalette (Primary, Text, Status)
  - Schatten (shadow-sm, shadow-md, shadow-primary)
  - Border Radius (sm, md, lg, full)
  - Spacing (xs, sm, md, lg, xl)
  - Typography (Font Families & Sizes)
  - SCSS Mixins (Flexbox, Buttons, Inputs)
  - Component Guidelines
  - Animation Guidelines

- 🏷️ **Naming Conventions**
  - Java: PascalCase, camelCase, UPPER_SNAKE_CASE
  - TypeScript/Vue: PascalCase, camelCase, kebab-case

---

### ⚡ [DESIGN_QUICK_REFERENCE.md](../DESIGN_QUICK_REFERENCE.md)
**Schnellübersicht für tägliche Entwicklung**

- Farben (SCSS Variablen)
- Spacing & Border Radius
- Schatten-Werte
- Java Quick Tips (Annotationen, Injection)
- Vue 3 Quick Tips (Composition API, SCSS)
- Mixins (flex-center, button-primary)
- Naming Conventions
- Code-Qualität Checklist

---

### 📋 [DESIGN_SUMMARY.txt](../DESIGN_SUMMARY.txt)
**Visuelle Terminal-Zusammenfassung**

ASCII-formatierte Übersicht mit:
- Verfügbare Dokumentation
- Design-System Kern-Elemente
- Java & Vue Quick Tips
- Naming Conventions
- Code-Qualität Checklist
- Wo finde ich was?

---

## 📝 Detaillierte Style-Guides

### Backend Code Style
**📄 [developer-code-style.md](../backend/openrouter-service/src/main/resources/prompts/developer-code-style.md)**

1017 Zeilen detaillierte Coding-Standards:
- ☕ **Java/Spring Boot**
  - Package Structure
  - Class Naming
  - Annotations Guide
  - Constructor vs. Field Injection
  - Logging (Slf4j)
  - REST Controllers
  - Service Layer
  - Repository
  - Configuration (@ConfigurationProperties)
  - WebClient (Reactive HTTP)
  - Exception Handling

- 🎨 **TypeScript/Vue 3**
  - Project Structure
  - Component Naming
  - Composition API (setup script)
  - TypeScript Types
  - API Clients
  - Composables
  - Pinia Stores

- 🐚 **Bash Scripts**
  - Shebang & Options
  - Colors & Output
  - Script Template
  - Error Handling

---

### Frontend Component Prompts
**📄 [Frontend_prompt.md](./development/Frontend_prompt.md)**

UI/UX Guidelines für Vue Components:
- **Chat Interface Layout**
- **Message Design** (User vs. AI)
- **Metadata Display** (Sources, Tools, Confidence)
- **Quick Actions** (Suggested Queries)
- **Loading States** (Typing Indicator)
- **Error Handling**
- **Styling Guidelines** (Colors, Shadows, Animations)

---

## 🎨 SCSS Style System

### Variablen
**📁 [_variables.scss](../frontend/src/styles/_variables.scss)**

```scss
// Farben
$primary-gradient: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
$primary-color: #667eea;
$text-dark: #2c3e50;
$success-color: #4caf50;

// Spacing
$spacing-xs: 0.25rem;  // 4px
$spacing-md: 1rem;     // 16px
$spacing-xl: 2rem;     // 32px

// Border Radius
$radius-md: 0.5rem;    // 8px
$radius-full: 2rem;    // 32px

// Schatten
$shadow-sm: 0 2px 5px rgba(0, 0, 0, 0.1);
$shadow-primary: 0 2px 8px rgba(102, 126, 234, 0.3);

// Typography
$font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
$font-size-base: 0.85rem;
$font-size-lg: 1rem;
```

---

### Mixins
**📁 [_mixins.scss](../frontend/src/styles/_mixins.scss)**

```scss
// Flexbox Helpers
@mixin flex-center { ... }
@mixin flex-between { ... }
@mixin flex-column { ... }

// Button Styles
@mixin button-primary { ... }
@mixin button-secondary { ... }
@mixin button-glass { ... }

// Input Fields
@mixin input-field { ... }
```

---

### Component-Spezifische Styles
**📁 [frontend/src/styles/](../frontend/src/styles/)**

- `_chat-interface.scss` - Chat-UI Styles
- `_team-assistant-chat.scss` - Team Assistant Chat
- `_openrouter-chat.scss` - OpenRouter Chat Interface
- `_support-chat.scss` - Support Chat
- `_components.scss` - Gemeinsame Components
- `_layout.scss` - Layout & Grid

---

## 🏷️ Naming Conventions Übersicht

### Java/Spring Boot

| Element | Convention | Beispiel |
|---------|-----------|----------|
| **Klassen** | PascalCase | `ChatService`, `OpenRouterClient` |
| **Controller** | `{Feature}Controller` | `ChatController` |
| **Service** | `{Feature}Service` | `ChatService` |
| **Repository** | `{Entity}Repository` | `ConversationRepository` |
| **Model/DTO** | `{Feature}Request/Response` | `ChatRequest`, `ChatResponse` |
| **Methoden** | camelCase | `processChat()`, `validateRequest()` |
| **Variablen** | camelCase | `userId`, `conversationId` |
| **Konstanten** | UPPER_SNAKE_CASE | `MAX_RETRIES`, `API_TIMEOUT` |
| **Packages** | lowercase | `com.example.openrouter.service` |

---

### TypeScript/Vue 3

| Element | Convention | Beispiel |
|---------|-----------|----------|
| **Components** | PascalCase | `ChatInterface.vue`, `MessageItem.vue` |
| **Composables** | `use{Feature}.ts` | `useChat.ts`, `useDevAssistant.ts` |
| **Stores** | `{feature}Store.ts` | `chatStore.ts` |
| **API Clients** | `{feature}.ts` | `chat.ts`, `dev-assistant.ts` |
| **Funktionen** | camelCase | `sendMessage()`, `formatDate()` |
| **Variablen** | camelCase | `userId`, `isLoading` |
| **Konstanten** | UPPER_SNAKE_CASE | `API_BASE_URL` |
| **Types/Interfaces** | PascalCase | `ChatRequest`, `Message` |
| **Dateien** | kebab-case | `chat-service.ts`, `dev-assistant.vue` |

---

### Bash Scripts

| Element | Convention | Beispiel |
|---------|-----------|----------|
| **Variablen** | UPPER_SNAKE_CASE | `PROJECT_ROOT`, `API_KEY` |
| **Funktionen** | snake_case | `log_info`, `check_requirements` |
| **Dateien** | kebab-case | `start-backend.sh`, `test-all.sh` |

---

## ✅ Code-Qualität Checklist

Verwende diese Checkliste vor jedem Commit:

### Backend (Java)
- [ ] Lombok Annotationen verwendet (@Slf4j, @Builder, @Data)
- [ ] Constructor Injection statt Field Injection
- [ ] Logging für wichtige Operationen hinzugefügt (log.info, log.error)
- [ ] Error Handling mit try-catch implementiert
- [ ] Input-Validierung vorhanden
- [ ] Keine hardcoded Werte (nutze @ConfigurationProperties)
- [ ] REST Controller folgt Namenskonventionen
- [ ] Service-Methoden sind gut dokumentiert

### Frontend (Vue/TypeScript)
- [ ] Composition API mit `<script setup lang="ts">` verwendet
- [ ] TypeScript Types/Interfaces definiert
- [ ] Props & Emits typisiert
- [ ] SCSS Variablen aus `_variables.scss` verwendet
- [ ] SCSS Mixins aus `_mixins.scss` verwendet
- [ ] Keine hardcoded Farben/Spacing-Werte
- [ ] Components folgen Namenskonventionen (PascalCase)
- [ ] Error Handling für API-Calls vorhanden

### Allgemein
- [ ] Naming Conventions befolgt
- [ ] Kommentare für komplexe Logik
- [ ] Konsistente Formatierung (Spaces, Indentation)
- [ ] Code ist lesbar und wartbar
- [ ] Keine unnötigen console.log() / System.out.println()

---

## 📖 Weitere Ressourcen

### Architecture Documentation
- [MCP Multi-Provider Architecture](./architecture/MCP_MULTI_PROVIDER_ARCHITECTURE.md)
- [OpenRouter Service Architecture](./architecture/OPENROUTER_SERVICE_ARCHITECTURE.md)
- [RAG MCP Integration](./architecture/RAG_MCP_INTEGRATION.md)
- [Conversation History Implementation](./architecture/CONVERSATION_HISTORY_IMPLEMENTATION.md)

### Feature Documentation
- [Features Index](../FEATURES_INDEX.md)
- [Meta-Prompting Feature](./features/META_PROMPTING_FEATURE.md)
- [Temperature Control Feature](./features/TEMPERATURE_FEATURE.md)

### Quickstarts
- [MCP Service Quickstart](./quickstarts/MCP_SERVICE_QUICKSTART.md)
- [OpenRouter Quickstart](./quickstarts/OPENROUTER_QUICKSTART.md)
- [Perplexity MCP Quickstart](./quickstarts/PERPLEXITY_MCP_QUICKSTART.md)

---

## 🚀 Workflow

### Für neue Features:

1. **Design Phase**
   - Lese relevante Guidelines (DESIGN_GUIDELINES.md)
   - Überprüfe SCSS Variablen für UI-Elemente
   - Folge Naming Conventions

2. **Implementation**
   - Backend: Nutze Constructor Injection, Lombok, Logging
   - Frontend: Composition API, TypeScript Types, SCSS Mixins
   - Nutze DESIGN_QUICK_REFERENCE.md für schnelle Lookups

3. **Review**
   - Checke Code-Qualität Checklist
   - Teste auf Konsistenz mit existierendem Code
   - Überprüfe Naming Conventions

4. **Documentation**
   - Aktualisiere Guidelines bei neuen Patterns
   - Dokumentiere neue Components/Services
   - Füge Beispiele hinzu

---

## 💡 Best Practices Summary

### Backend
✅ **DO:**
- Constructor Injection für Dependencies
- Lombok (@Slf4j, @Builder, @Data)
- Logging mit aussagekräftigen Messages
- Validierung von Input-Daten
- Exception Handling mit Custom Exceptions

❌ **DON'T:**
- Field Injection (@Autowired auf Feldern)
- System.out.println() statt Logging
- Hardcoded Werte ohne Configuration
- Leere catch-Blöcke

### Frontend
✅ **DO:**
- Composition API (`<script setup lang="ts">`)
- TypeScript Types für alles
- SCSS Variablen & Mixins verwenden
- Props & Emits typisieren
- Error Handling für API-Calls

❌ **DON'T:**
- Options API (alt)
- `any` Type in TypeScript
- Inline Styles oder hardcoded Farben
- Ungetypte Props/Emits
- Unbehandelte Promise-Rejections

---

## 📞 Support

Bei Fragen zu den Design-Richtlinien:

1. Prüfe zuerst **DESIGN_GUIDELINES.md** und **DESIGN_QUICK_REFERENCE.md**
2. Schaue in existierenden Code für Beispiele
3. Öffne ein Issue im Repository mit dem Label `documentation`

---

**Letzte Aktualisierung:** 2026-01-17  
**Version:** 1.0.0

