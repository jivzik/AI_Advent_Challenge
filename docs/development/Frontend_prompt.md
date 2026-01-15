# Prompt: Create TeamAssistantChat.vue Component

## Overview
Create a Vue 3 component called **TeamAssistantChat.vue** that provides a chat interface for developers to interact with an AI Team Assistant. The assistant helps with project documentation, task management, bug tracking, and priority analysis.

## Technical Requirements

### Technology Stack
- Vue 3 Composition API (setup script)
- TypeScript
- Axios for HTTP requests
- Tailwind CSS for styling
- Lucide Vue icons (optional)

### API Endpoint
**POST** `http://localhost:8089/api/team-assistant/query`

**Request:**
```json
{
  "userEmail": "dev@company.com",
  "query": "How does authentication work?",
  "sessionId": "uuid-here" // optional
}
```

**Response:**
```json
{
  "answer": "Based on the documentation...\n\n---\n\n📚 **Источники:**\n1. `ARCHITECTURE.md`\n2. `API.md`",
  "queryType": "ANSWER_QUESTION",
  "sources": ["ARCHITECTURE.md", "API.md"],
  "toolsUsed": ["rag:search_documents"],
  "actions": [],
  "confidenceScore": 0.95,
  "responseTimeMs": 15000,
  "timestamp": "2026-01-15T21:00:00"
}
```

## Component Structure

### Main Features
1. **Chat Interface**
    - Message history (user questions + AI responses)
    - Input field for user queries
    - Send button
    - Auto-scroll to bottom
    - Loading indicator while AI responds

2. **Message Display**
    - User messages: right-aligned, blue background
    - AI responses: left-aligned, gray background
    - Markdown support for AI answers (formatting, lists, code blocks)
    - Preserve line breaks and formatting from API

3. **Metadata Display** (for AI responses)
    - **Sources badge**: Show list of documents used
        - Icon: 📚 or document icon
        - Expandable list: "ARCHITECTURE.md", "API.md"
        - Click to highlight in message

    - **Tools Used badge**: Show tools called
        - Icon: 🔧 or tool icon
        - Examples: "rag:search_documents", "git:list_github_issues"
        - Color-coded by tool type

    - **Confidence Score**: Visual indicator
        - 0.9-1.0: Green badge "High confidence"
        - 0.7-0.89: Yellow badge "Medium confidence"
        - <0.7: Orange badge "Low confidence"

    - **Query Type badge**: Show type of query
        - ANSWER_QUESTION, SHOW_TASKS, CREATE_TASK, etc.
        - Color-coded

    - **Response Time**: Small gray text "Responded in 15s"

4. **Quick Actions / Suggested Queries**
    - Show 4-6 example queries as buttons
    - Examples:
        - "How does authentication work?"
        - "What are the critical bugs?"
        - "Show project architecture"
        - "What should I work on today?"
        - "Explain the payment flow"
        - "List open GitHub issues"
    - Click to auto-fill and send

5. **User Context**
    - Small form at top or in settings:
        - Email input (default: "dev@company.com")
        - Role selector: Developer, PM, Designer, QA
        - Team selector: Backend, Frontend, DevOps
    - Saved to localStorage

## UI/UX Requirements

### Layout
```
┌─────────────────────────────────────┐
│  Team Assistant                  [⚙️] │  ← Header
├─────────────────────────────────────┤
│  📚 Quick Actions:                  │
│  [How does auth work?] [Critical bugs] │
│  [Architecture] [What to work on?]   │
├─────────────────────────────────────┤
│                                     │
│  [User message]          👤         │  ← Messages
│                                     │
│  🤖  [AI response]                  │
│      Sources: ARCHITECTURE.md       │
│      Tools: rag:search_documents    │
│      Confidence: ● High (0.95)      │
│      15s                            │
│                                     │
│  [User message]          👤         │
│                                     │
│  🤖  [AI response with sources...]  │
│                                     │
├─────────────────────────────────────┤
│  [Type your question...]      [Send]│  ← Input
└─────────────────────────────────────┘
```

### Styling Guidelines
- Clean, professional look
- Rounded corners for messages
- Subtle shadows
- Smooth animations (fade in for new messages)
- Responsive (works on desktop and tablet)
- Good contrast for readability
- Use color coding:
    - Blue: User messages
    - Gray: AI messages
    - Green: Success/high confidence
    - Yellow: Medium confidence
    - Orange: Low confidence
    - Purple: Tools used
    - Cyan: Sources

### Loading State
- Show typing indicator (3 animated dots) while waiting
- Disable input and send button
- Show "AI is thinking..." text

### Error Handling
- Network errors: Show friendly error message
- API errors: Display error from server
- Retry button for failed requests
- Toast notifications for errors

## Example Interactions

### Interaction 1: Documentation Question
```
User: How does authentication work?

AI: Based on the documentation:

1. **Login Flow:**
   - User submits credentials
   - Auth Service validates
   - JWT token generated (1h expiry)

---

📚 **Источники:**
1. `ARCHITECTURE.md`
2. `API.md`

[Metadata badges shown below message]
```

### Interaction 2: Bug Question
```
User: What critical bugs should I fix?

AI: 🐛 Critical Bugs:

From Technical Debt:
1. Payment retry logic (TASK-234)

From GitHub Issues:
1. [#234] Payment fails on retry

---

📚 **Источники:**
1. `TECHNICAL_DEBT.md`
2. GitHub Issues

[Shows sources + toolsUsed: rag:search_documents, git:list_github_issues]
```

### Interaction 3: Simple Question
```
User: Hello

AI: Hello! How can I help you today?

[No sources/tools shown, lower confidence]
```

## State Management

### Component State
```typescript
interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  metadata?: {
    sources?: string[];
    toolsUsed?: string[];
    confidenceScore?: number;
    queryType?: string;
    responseTimeMs?: number;
  };
}

interface UserContext {
  email: string;
  role: string;
  team: string;
}

const messages = ref<Message[]>([]);
const currentQuery = ref('');
const isLoading = ref(false);
const userContext = ref<UserContext>({
  email: 'dev@company.com',
  role: 'developer',
  team: 'backend'
});
```

### Persistence
- Save messages to localStorage (last 50 messages)
- Save user context to localStorage
- Clear chat button (clears history)
- Export chat button (download as markdown/json)

## Advanced Features (Optional)

1. **Message Actions**
    - Copy message button
    - Regenerate response button
    - Thumbs up/down feedback

2. **Search in Chat**
    - Search box to filter messages
    - Highlight search terms

3. **Voice Input** (future)
    - Microphone button
    - Speech-to-text

4. **Code Highlighting**
    - If answer contains code blocks
    - Use syntax highlighting library

5. **Source Preview**
    - Click on source name
    - Show snippet from document

## Testing Scenarios

Test the component with these queries:

1. **Documentation:** "How does authentication work?"
2. **Bugs:** "What critical bugs exist?"
3. **API:** "What is the endpoint for creating orders?"
4. **Architecture:** "Explain the system architecture"
5. **Empty:** "Hello"
6. **Complex:** "What should I work on today based on priorities?"

## Acceptance Criteria

✅ Chat interface displays correctly
✅ Messages are sent to API and responses displayed
✅ Loading indicator shows while waiting
✅ Metadata badges display correctly
✅ Sources and tools are shown
✅ Confidence score is visualized
✅ Quick action buttons work
✅ User context is configurable
✅ Messages persist in localStorage
✅ Error handling works
✅ Responsive on different screen sizes
✅ Markdown formatting in AI responses
✅ Auto-scroll to bottom on new message

## File Structure

Create these files:
1. `TeamAssistantChat.vue` - Main component
2. `types/team-assistant.ts` - TypeScript interfaces
3. `composables/useTeamAssistant.ts` - API logic (optional)

## Example Code Structure (skeleton)

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from 'axios';

interface Message {
  // ... types
}

const messages = ref<Message[]>([]);
const currentQuery = ref('');
const isLoading = ref(false);

const sendQuery = async () => {
  // Send to API
  // Add to messages
  // Handle response
};

const quickActions = [
  "How does authentication work?",
  "What are the critical bugs?",
  // ...
];

onMounted(() => {
  // Load from localStorage
});
</script>

<template>
  <div class="team-assistant-chat">
    <!-- Header -->
    <div class="header">
      <h2>Team Assistant</h2>
    </div>

    <!-- Quick Actions -->
    <div class="quick-actions">
      <button v-for="action in quickActions" @click="currentQuery = action">
        {{ action }}
      </button>
    </div>

    <!-- Messages -->
    <div class="messages">
      <div v-for="message in messages" :class="message.role">
        <!-- Message content -->
        <!-- Metadata badges -->
      </div>
    </div>

    <!-- Input -->
    <div class="input-area">
      <input v-model="currentQuery" @keyup.enter="sendQuery" />
      <button @click="sendQuery" :disabled="isLoading">
        Send
      </button>
    </div>
  </div>
</template>

<style scoped>
/* Tailwind CSS classes */
</style>
```

## Success Metrics

After implementation, the component should:
- ✅ Handle 100% of API response formats correctly
- ✅ Display all metadata fields when present
- ✅ Load in <100ms (excluding API call)
- ✅ Work smoothly with 50+ messages in history
- ✅ Be intuitive for non-technical users
- ✅ Match the design system of the main app

## Notes

- Focus on clean, readable code
- Add comments for complex logic
- Use TypeScript for type safety
- Follow Vue 3 best practices
- Make it accessible (ARIA labels)
- Test with real API responses
- Handle edge cases (empty sources, long responses, etc.)

---

**Ready to implement!** 🚀