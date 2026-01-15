# TeamAssistantChat Component

## 📋 Overview
The **TeamAssistantChat** component provides a chat interface for developers to interact with an AI Team Assistant. It helps with project documentation, task management, bug tracking, and priority analysis.

## 🎯 Features

### Core Features
- ✅ Real-time chat interface with AI Team Assistant
- ✅ User context configuration (email, role, team)
- ✅ Quick action buttons for common queries
- ✅ Markdown rendering support in AI responses
- ✅ Metadata display (sources, tools, confidence score)
- ✅ LocalStorage persistence for messages and user context
- ✅ Loading indicators and error handling
- ✅ Responsive design

### Metadata Display
- **Sources**: Documents used by RAG system
- **Tools Used**: AI tools called (e.g., `rag:search_documents`, `git:list_github_issues`)
- **Confidence Score**: Visual indicator (High/Medium/Low)
- **Query Type**: Type of query processed
- **Response Time**: Time taken to generate response

## 📁 File Structure

```
frontend/src/
├── components/
│   └── TeamAssistantChat.vue          # Main component
├── services/
│   └── teamAssistantService.ts        # API service
├── types/
│   └── team-assistant.ts              # TypeScript interfaces
└── styles/
    └── _team-assistant-chat.scss      # Component styles
```

## 🚀 Usage

### In App.vue
```vue
<template>
  <TeamAssistantChat />
</template>

<script setup lang="ts">
import TeamAssistantChat from './components/TeamAssistantChat.vue';
</script>
```

### API Endpoint
The component expects the backend API at:
```
POST http://localhost:8089/api/team-assistant/query
```

### Request Format
```json
{
  "userEmail": "dev@company.com",
  "query": "How does authentication work?",
  "sessionId": "session-123" 
}
```

### Response Format
```json
{
  "answer": "Based on the documentation...",
  "queryType": "ANSWER_QUESTION",
  "sources": ["ARCHITECTURE.md", "API.md"],
  "toolsUsed": ["rag:search_documents"],
  "actions": [],
  "confidenceScore": 0.95,
  "responseTimeMs": 1500,
  "timestamp": "2026-01-15T21:00:00"
}
```

## 🎨 Styling

The component uses SCSS modules following the existing design system:
- Variables from `_variables.scss`
- Mixins from `_mixins.scss`
- Green gradient theme (Team Assistant branding)
- Responsive design for mobile and tablet

### Color Scheme
- **Primary**: Green gradient (`#10b981` → `#059669`)
- **User Messages**: Blue gradient
- **Assistant Messages**: White with border
- **Badges**: Color-coded by type (sources, tools, confidence)

## 🔧 Configuration

### User Context
Users can configure their profile:
- **Email**: User's email address
- **Role**: Developer, PM, Designer, QA
- **Team**: Backend, Frontend, DevOps, Mobile

Settings are persisted in localStorage.

### Quick Actions
Pre-configured queries:
- "How does authentication work?"
- "What are the critical bugs?"
- "Show project architecture"
- "What should I work on today?"
- "Explain the payment flow"
- "List open GitHub issues"

## 💾 LocalStorage Persistence

### Saved Data
- **User Context**: `teamAssistantUserContext`
- **Messages**: `teamAssistantMessages` (last 50 messages)

### Data Structure
```typescript
// User context
{
  email: string;
  role: 'developer' | 'pm' | 'designer' | 'qa';
  team: 'backend' | 'frontend' | 'devops' | 'mobile';
}

// Messages
[
  {
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
    }
  }
]
```

## 🧪 Testing

### Manual Testing
1. Start the backend service on port 8089
2. Start the frontend with `npm run dev`
3. Navigate to "Team Assistant" mode
4. Try the quick actions
5. Send custom queries
6. Check metadata display
7. Verify localStorage persistence

### Test Queries
```
✅ Documentation: "How does authentication work?"
✅ Bugs: "What critical bugs exist?"
✅ API: "What is the endpoint for creating orders?"
✅ Architecture: "Explain the system architecture"
✅ Simple: "Hello"
✅ Complex: "What should I work on today based on priorities?"
```

## 🎯 TypeScript Types

### Main Interfaces
```typescript
interface TeamAssistantMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  metadata?: TeamAssistantMetadata;
}

interface TeamAssistantMetadata {
  sources?: string[];
  toolsUsed?: string[];
  confidenceScore?: number;
  queryType?: QueryType;
  responseTimeMs?: number;
  actions?: string[];
}

interface UserContext {
  email: string;
  role: 'developer' | 'pm' | 'designer' | 'qa';
  team: 'backend' | 'frontend' | 'devops' | 'mobile';
}
```

## 📊 Metadata Badges

### Confidence Score
- **High (0.9-1.0)**: Green badge
- **Medium (0.7-0.89)**: Yellow badge
- **Low (<0.7)**: Orange badge

### Query Types
- `ANSWER_QUESTION`
- `SHOW_TASKS`
- `CREATE_TASK`
- `ANALYZE_PRIORITY`
- `EXPLAIN_ARCHITECTURE`
- `LIST_BUGS`
- `SEARCH_DOCS`
- `GENERAL`

### Tool Colors
- **RAG tools** (`rag:*`): Purple
- **Git tools** (`git:*`): Cyan
- **Search tools** (`search:*`): Blue
- **Task tools** (`task:*`): Green
- **Other**: Gray

## 🔍 Troubleshooting

### Backend Connection Issues
```
Error: Failed to get response from Team Assistant
Solution: Ensure backend is running on port 8089
```

### CORS Issues
```
Error: CORS policy blocked
Solution: Configure backend CORS headers
```

### LocalStorage Full
```
Error: QuotaExceededError
Solution: Clear localStorage or reduce message history
```

## 🚀 Future Enhancements

### Planned Features
- [ ] Message search and filtering
- [ ] Export chat history (Markdown/JSON)
- [ ] Voice input (speech-to-text)
- [ ] Code syntax highlighting
- [ ] Source document preview
- [ ] Message feedback (thumbs up/down)
- [ ] Regenerate response
- [ ] Copy message to clipboard

### Integration Ideas
- [ ] Connect to GitHub Issues API
- [ ] Integrate with JIRA for task management
- [ ] Add Slack notifications
- [ ] Calendar integration for task deadlines

## 📝 Contributing

When contributing to this component:
1. Follow Vue 3 Composition API best practices
2. Use TypeScript for type safety
3. Follow existing SCSS structure
4. Add JSDoc comments for functions
5. Test with real API responses
6. Update this README for significant changes

## 📄 License

Part of the AI Advent Challenge project.

---

**Created**: 2026-01-15  
**Last Updated**: 2026-01-15  
**Status**: ✅ Ready for use

