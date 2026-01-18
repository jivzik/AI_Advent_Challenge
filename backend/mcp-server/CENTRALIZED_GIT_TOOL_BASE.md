# 🎯 Централизованная архитектура GitToolBase

## ✅ Что сделано

Создана централизованная база для всех Git и GitHub tools через класс `GitToolBase`.

---

## 🏗️ Архитектура

### GitToolBase - Центральный класс

```java
@Slf4j
public abstract class GitToolBase {
    
    @Value("${git.project.root:#{systemProperties['user.dir']}}")
    protected String projectRoot;

    @Value("${personal.github.token}")
    protected String githubToken;

    @Value("${personal.github.repository}")
    protected String defaultRepository;
    
    /**
     * Подключение к GitHub API с использованием токена
     */
    protected GitHub connectToGitHub() throws IOException {
        if (githubToken != null && !githubToken.isBlank()) {
            return new GitHubBuilder()
                    .withOAuthToken(githubToken)
                    .build();
        } else {
            return GitHub.connectAnonymously();
        }
    }
    
    /**
     * Получить имя репозитория (default или custom)
     */
    protected String getRepository(String customRepository) {
        if (customRepository != null && !customRepository.isBlank()) {
            return customRepository;
        }
        return defaultRepository;
    }
    
    /**
     * Получить Git-репозиторий (JGit)
     */
    protected Git getGitRepository() {
        // ...implementation
    }
}
```

---

## 📋 Все tools теперь используют GitToolBase

### ✅ GitHub API Tools (extends GitToolBase)
- `TriggerWorkflowTool` - trigger GitHub Actions workflows ✨ NEW
- `ListCommitsTool` - list commits from repository ✨ NEW
- `CreateGitHubIssueTool` - create GitHub issues
- `ListGitHubIssuesTool` - list GitHub issues
- `UpdateGitHubIssueTool` - update GitHub issues
- `DeleteGitHubIssueTool` - delete GitHub issues
- `ListOpenPRsTool` - list open pull requests
- `GetPRInfoTool` - get PR information
- `PostPRReviewTool` - post PR reviews

### ✅ Local Git Tools (extends GitToolBase)
- `GitAddTool` - stage files (git add) ✨ NEW
- `GitCommitTool` - create commits (git commit) ✨ NEW
- `GitPushTool` - push to remote (git push) ✨ NEW + Auth with githubToken
- `GetGitLogTool` - get git log
- `GetGitStatusTool` - get git status
- `GetCurrentBranchTool` - get current branch

### ✅ File Tools (extends GitToolBase)
- `ReadProjectFileTool` - read project files
- `ListProjectFilesTool` - list project files
- `CompareBranchesTool` - compare branches

---

## 🎯 Преимущества централизации

### 1. **Единая точка аутентификации**
```java
// Все tools используют один метод
GitHub github = connectToGitHub();
```
- ✅ Один GitHub token для всех
- ✅ Централизованное логирование
- ✅ Единая обработка ошибок

### 2. **Централизованный репозиторий**
```java
// Можно использовать default или передать custom
String repo = getRepository(customRepo);
```
- ✅ `personal.github.repository` из properties
- ✅ Можно override в каждом tool
- ✅ Валидация в одном месте

### 3. **Переиспользование кода**
```java
// JGit для локальных операций
Git git = getGitRepository();
git.add().addFilepattern(".").call();
git.commit().setMessage("message").call();
git.push().setCredentialsProvider(credentials).call();
```

### 4. **Единая конфигурация**
```properties
# application.properties
personal.github.token=${PERSONAL_GITHUB_TOKEN}
personal.github.repository=jivzik/AI_Advent_Challenge
git.project.root=${user.dir}
```

---

## 🔧 Как создать новый Git Tool

### Пример: NewGitHubTool

```java
@Component
@Slf4j
public class NewGitHubTool extends GitToolBase implements Tool {
    private static final String NAME = "new_github_tool";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
        // Define input schema
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        
        // Используем централизованные методы
        String repo = getRepository(args.getString("repository", null));
        
        try {
            GitHub github = connectToGitHub();
            GHRepository repository = github.getRepository(repo);
            
            // Твоя логика здесь
            
            return result;
        } catch (Exception e) {
            log.error("Failed: {}", e.getMessage(), e);
            throw new ToolExecutionException("Failed: " + e.getMessage(), e);
        }
    }
}
```

### Преимущества:
- ✅ Не нужно дублировать `@Value` поля
- ✅ Не нужно писать `connectToGitHub()`
- ✅ Не нужно валидировать repository
- ✅ Автоматически получаешь все helper методы

---

## 📊 Статистика

### Было (дублирование):
```java
// В каждом tool
@Value("${personal.github.token}")
private String githubToken;

@Value("${personal.github.repository}")
private String defaultRepository;

private GitHub connectToGitHub() throws IOException {
    return new GitHubBuilder()
        .withOAuthToken(githubToken)
        .build();
}
```
❌ **Дублирование в 15+ файлах**

### Стало (централизация):
```java
// Только в GitToolBase
protected GitHub connectToGitHub() { ... }
protected String getRepository(String custom) { ... }
protected Git getGitRepository() { ... }
```
✅ **Один раз в GitToolBase, используется везде**

---

## 🚀 Результат

### Теперь все Git/GitHub tools:
1. ✅ **Используют единую аутентификацию** через `connectToGitHub()`
2. ✅ **Работают с одним репозиторием** через `getRepository()`
3. ✅ **Имеют доступ к JGit** через `getGitRepository()`
4. ✅ **Не дублируют код** - всё в `GitToolBase`
5. ✅ **Легко тестировать** - мокаем `GitToolBase`
6. ✅ **Легко расширять** - просто extends `GitToolBase`

---

## 🎉 Clean Architecture достигнута!

**DRY (Don't Repeat Yourself):** ✅  
**Single Responsibility:** ✅  
**Open/Closed Principle:** ✅  
**Dependency Inversion:** ✅  

Все Git/GitHub tools теперь используют централизованную логику из `GitToolBase`!

---

**Дата:** 2026-01-18  
**Статус:** ✅ Реализовано и протестировано  
**Компиляция:** ✅ BUILD SUCCESS

