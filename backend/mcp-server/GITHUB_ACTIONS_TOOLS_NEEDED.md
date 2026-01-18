# 🎯 AI DevOps CLI - Требуемые GitHub Actions Tools для MCP Server

## ❌ Проблема

MCP Server не имеет tools для GitHub Actions. При вызове `trigger_workflow` получаем ошибку:
```
Unbekanntes Tool: trigger_workflow
```

## ✅ Решение

Нужно добавить 5 новых tools в MCP Server:

### 1. **TriggerWorkflowTool**
- **Имя:** `trigger_workflow`
- **Описание:** Trigger a GitHub Actions workflow
- **Параметры:**
  - `workflow` (string, required): Workflow file name (e.g., deploy.yml)
  - `ref` (string, default: "main"): Git branch/tag
- **Использует:** `org.kohsuke.github.GHWorkflow.dispatch(ref)`

### 2. **ListWorkflowRunsTool**
- **Имя:** `list_workflow_runs`
- **Описание:** List recent workflow runs
- **Параметры:**
  - `workflow` (string, required): Workflow file name
  - `limit` (integer, default: 10): Number of runs to return
- **Использует:** `GHWorkflow.listRuns()`

### 3. **ListCommitsTool**
- **Имя:** `list_commits`
- **Описание:** List commits from repository
- **Параметры:**
  - `limit` (integer, default: 30): Number of commits
  - `since` (string, optional): Date filter (ISO 8601)
- **Использует:** `GHRepository.listCommits()`

### 4. **GitAddTool**
- **Имя:** `git_add`
- **Описание:** Stage files for commit (git add)
- **Параметры:**
  - `path` (string, default: "."): Path to add
- **Использует:** JGit `git.add().addFilepattern(path).call()`

### 5. **GitCommitTool**
- **Имя:** `git_commit`
- **Описание:** Create a git commit
- **Параметры:**
  - `message` (string, required): Commit message
- **Использует:** JGit `git.commit().setMessage(message).call()`

---

## 📝 Шаблон реализации

```java
package de.jivz.mcp.tools.git;

import de.jivz.mcp.tools.Tool;
import de.jivz.mcp.tools.ToolArguments;
import de.jivz.mcp.tools.ToolExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class TriggerWorkflowTool extends GitToolBase implements Tool {

    @Override
    public String getName() {
        return "trigger_workflow";
    }

    @Override
    public String getDescription() {
        return "Trigger a GitHub Actions workflow by file name";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        // Define JSON schema for inputs
        // Required: workflow, ref
    }

    @Override
    public Object execute(ToolArguments arguments) throws ToolExecutionException {
        String workflowFileName = arguments.getRequiredString("workflow");
        String ref = arguments.getStringOrDefault("ref", "main");
        
        GitHub github = getGitHub();
        GHWorkflow workflow = github.getRepository(getRepository())
            .getWorkflow(workflowFileName);
        
        workflow.dispatch(ref);
        
        return Map.of(
            "success", true,
            "workflow", workflowFileName,
            "ref", ref
        );
    }
}
```

---

## 🔧 Как добавить

### Вариант 1: Создать файлы вручную

1. Скопируй шаблоны из этой документации
2. Создай 5 файлов в `backend/mcp-server/src/main/java/de/jivz/mcp/tools/git/`
3. Собери: `mvn clean package`
4. Перезапусти MCP Server

### Вариант 2: Использовать существующие GetGitLogTool как пример

```bash
cd backend/mcp-server/src/main/java/de/jivz/mcp/tools/git
# Скопируй GetGitLogTool.java и адаптируй
```

---

## 🚀 После добавления

### Перезапусти MCP Server

```bash
cd infra/prod
docker-compose restart mcp-server

# или
cd backend/mcp-server
mvn spring-boot:run
```

### Проверь что tools зарегистрированы

```bash
curl http://localhost:8081/mcp/tools
```

Должен вернуть список включающий:
- `trigger_workflow`
- `list_workflow_runs`
- `list_commits`
- `git_add`
- `git_commit`

---

## ✅ После добавления tools CLI команды заработают:

```bash
> deploy team-service
🔄 Deploying team-service...
✅ Workflow triggered successfully

> commit "feat: Add GitHub Actions tools"
✅ Changes committed successfully

> generate release notes
📝 Analyzing commits...
📝 Generated release notes
```

---

## 📚 Dependencies (уже есть в pom.xml)

- `org.kohsuke:github-api` - для GitHub Actions API
- `org.eclipse.jgit:org.eclipse.jgit` - для git operations

---

**Статус:** Tools нужно добавить вручную (create_file tool создает файлы в неправильном порядке)
**Приоритет:** HIGH (блокирует deploy и commit команды)
**Время:** ~30 минут на создание 5 файлов

