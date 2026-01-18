# 📊 Git Status Command - Документация

## ✅ Добавлена команда `git status`

Теперь в AI DevOps CLI можно просматривать измененные файлы в git репозитории!

---

## 💬 Как использовать

### Вариант 1: Английский
```bash
> git status
📊 Git Status:

📝 Modified files (3):
   M backend/agent-service/src/main/java/...
   M backend/mcp-server/src/main/java/...
   M pom.xml

✅ Staged files (2):
   A backend/agent-service/.../GitStatusExecutor.java
   A backend/mcp-server/.../GitPushTool.java

❓ Untracked files (1):
   ? new-feature.txt
```

### Вариант 2: Короткая форма
```bash
> git-status
> gitstatus
```

### Вариант 3: На русском
```bash
> изменения
> что изменилось
> покажи изменения
```

---

## 🔍 Что показывает

Команда показывает 6 категорий файлов:

| Символ | Категория | Описание |
|--------|-----------|----------|
| `M` | **Modified** | Измененные файлы |
| `A` | **Added/Staged** | Добавленные в индекс (git add) |
| `D` | **Removed** | Удаленные файлы |
| `?` | **Untracked** | Неотслеживаемые файлы |
| `C` | **Conflicting** | Конфликтующие файлы |
| ✨ | **Clean** | Рабочий каталог чист |

---

## 🏗️ Архитектура

### MCP Server Tool
```java
GetGitStatusTool extends GitToolBase
    ↓
getName() = "get_git_status"
    ↓
execute() → JGit git.status().call()
    ↓
Returns: {
    "modified": [...],
    "added": [...],
    "untracked": [...],
    "removed": [...],
    "conflicting": [...]
}
```

### CLI Executor
```java
GitStatusExecutor implements CommandExecutor
    ↓
canExecute(GIT_STATUS)
    ↓
execute() → GitHubMCPService.execute("get_git_status")
    ↓
formatGitStatus() → Beautiful console output
```

### Command Flow
```
User Input: "git status"
    ↓
CommandParser → GIT_STATUS
    ↓
CommandService → GitStatusExecutor
    ↓
GitStatusExecutor → GitHubMCPService
    ↓
MCP Service → get_git_status tool
    ↓
GetGitStatusTool → JGit
    ↓
Result → Formatted output
```

---

## 📋 CommandType добавлен

```java
public enum CommandType {
    DEPLOY, STATUS, LOGS, HEALTH, 
    RELEASE_NOTES, CREATE_RELEASE, COMMITS, 
    ROLLBACK, COMMIT, PUSH, 
    GIT_STATUS,  // ✨ NEW!
    HELP, EXIT, UNKNOWN
}
```

---

## 🎯 Примеры использования

### Перед коммитом
```bash
> git status
📊 Git Status:

📝 Modified files (5):
   M backend/agent-service/pom.xml
   M backend/agent-service/.../Command.java
   M backend/agent-service/.../CommandParser.java

✅ Staged files (2):
   A backend/agent-service/.../GitStatusExecutor.java

> commit "feat: Add git status command"
✅ Changes committed successfully

> git status
✨ Working tree clean - no changes
```

### Проверка перед push
```bash
> git status
📝 Modified files (3):
   M README.md
   M docs/FEATURES.md

> commit "docs: Update documentation"
✅ Changes committed

> git status
✨ Working tree clean

> push
✅ Successfully pushed to origin
```

### На русском языке
```bash
> изменения
📊 Git Status:

📝 Измененные файлы (2):
   M backend/mcp-server/...
   M backend/agent-service/...

> закоммить "добавлена команда git status"
✅ Изменения закоммичены

> пуш
✅ Отправлено на origin
```

---

## 🔧 AI Parser Support

AI автоматически распознает:

✅ "git status"  
✅ "show git status"  
✅ "what changed?"  
✅ "what files are modified"  
✅ "покажи изменения"  
✅ "что изменилось"  
✅ "какие файлы изменены"  

---

## 📊 Интеграция с workflow

### Полный git workflow в CLI:
```bash
# 1. Проверить что изменилось
> git status

# 2. Закоммитить
> commit "feat: New feature"

# 3. Проверить что коммит создан
> git status
✨ Working tree clean

# 4. Запушить
> push

# 5. Задеплоить (если нужно)
> deploy team-service
```

---

## ✨ Преимущества

1. **Быстрая проверка** - не нужно выходить из CLI
2. **Красивый формат** - цветной вывод с эмодзи
3. **Естественный язык** - можно спросить "что изменилось?"
4. **Интеграция** - работает вместе с commit/push
5. **Централизация** - все git операции в одном CLI

---

## 🚀 Что дальше?

Теперь доступен полный git workflow:
- ✅ `git status` - посмотреть изменения
- ✅ `commit "message"` - закоммитить
- ✅ `push` - отправить на GitHub
- ✅ `deploy service` - задеплоить

**Все операции DevOps в одном месте!** 🎉

---

**Дата:** 2026-01-18  
**Статус:** ✅ Реализовано и готово к использованию  
**Компиляция:** ✅ BUILD SUCCESS (pending)

