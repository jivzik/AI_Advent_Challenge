# MCP Docker Monitor

**MCP Server для мониторинга Docker контейнеров через SSH**

## Описание

`mcp-docker-monitor` - это MCP (Model Context Protocol) сервер, написанный на Java 21 с использованием Spring Boot 3.x. Сервер подключается к удаленным серверам через SSH и предоставляет инструменты для мониторинга Docker контейнеров, чтения логов и анализа состояния приложений.

## Технологический стек

- **Java 21** - современная версия языка с latest features
- **Spring Boot 3.3.0** - framework для создания приложений
- **Maven** - build system и dependency management
- **Apache MINA SSHD 2.12.0** - SSH клиент для подключения к удаленным серверам
- **EdDSA 0.3.0** - поддержка современных ключей (ED25519, RSA SHA256)
- **Jackson** - JSON processing и serialization
- **Logback** - логирование

## Структура проекта

```
mcp-docker-monitor/
├── pom.xml                                    # Maven конфигурация
├── README.md                                  # Документация (этот файл)
├── src/
│   ├── main/
│   │   ├── java/com/ai/advent/mcp/docker/
│   │   │   ├── DockerMonitorMcpApplication.java      # Main application entry point
│   │   │   ├── config/
│   │   │   │   └── AppConfig.java                    # Spring Boot конфигурация
│   │   │   ├── controller/
│   │   │   │   └── *.java                            # REST контроллеры для MCP инструментов
│   │   │   ├── exception/
│   │   │   │   └── *.java                            # Custom exceptions
│   │   │   ├── service/
│   │   │   │   ├── DockerMonitorService.java         # Основной сервис
│   │   │   │   ├── DockerService.java                # Бизнес-логика работы с Docker
│   │   │   │   └── ToolsDefinitionService.java       # Определение MCP инструментов
│   │   │   ├── ssh/
│   │   │   │   └── SshManager.java                   # SSH connection manager (Apache MINA SSHD)
│   │   │   └── model/
│   │   │       ├── ContainerInfo.java                # Data model для контейнера
│   │   │       ├── ToolCallRequest.java              # MCP tool call request
│   │   │       ├── ToolCallResponse.java             # MCP tool call response
│   │   │       └── PerplexityFunctionDefinition.java # Tool definition model
│   │   └── resources/
│   │       └── application.yml                       # Конфигурация приложения
│   └── test/                                         # Unit tests
└── .gitignore                                # Git ignore rules
```

## Установка и запуск

### Требования

- Java 21 или выше
- Maven 3.8 или выше
- Docker на удаленном сервере (доступный через SSH)

### Сборка проекта

```bash
# Из корневой директории проекта
cd backend/mcp-docker-monitor

# Сборка Maven
mvn clean package

# Или для развития
mvn clean install -DskipTests
```

### Запуск сервера

```bash
# Запуск приложения
java -jar target/mcp-docker-monitor-1.0.0.jar

# Или используя Maven
mvn spring-boot:run
```

Сервер будет запущен на `http://localhost:8765` и начнет слушать MCP запросы через stdio transport.

## Доступные MCP инструменты

### 1. `list_docker_containers`
Список всех Docker контейнеров на удаленном сервере.

**Параметры:**
- `host` (string, required) - IP адрес или hostname удаленного сервера
- `port` (integer, optional) - SSH порт (по умолчанию 22)
- `username` (string, required) - SSH username

**Пример использования:**
```json
{
  "tool": "list_docker_containers",
  "arguments": {
    "host": "192.168.1.100",
    "username": "deploy",
    "port": 22
  }
}
```

**Результат:**
```
Found 3 containers:
- web-app (abc123def456) [running] Up 2 hours
- database (def456ghi789) [running] Up 5 hours
- redis (ghi789jkl012) [exited] Exited (0) 1 hour ago
```

### 2. `get_container_logs`
Получить логи из конкретного контейнера.

**Параметры:**
- `host` (string, required) - IP адрес или hostname сервера
- `port` (integer, optional) - SSH порт (по умолчанию 22)
- `username` (string, required) - SSH username
- `container_id` (string, required) - ID или имя контейнера
- `lines` (integer, optional) - Количество строк логов (по умолчанию 50)

**Пример использования:**
```json
{
  "tool": "get_container_logs",
  "arguments": {
    "host": "192.168.1.100",
    "username": "deploy",
    "container_id": "web-app",
    "lines": 100
  }
}
```

### 3. `analyze_container_logs`
Анализ логов контейнера на предмет ошибок и предупреждений.

**Параметры:**
- `host` (string, required)
- `port` (integer, optional)
- `username` (string, required)
- `container_id` (string, required)
- `lines` (integer, optional) - Количество строк для анализа (по умолчанию 50)

**Результат:**
```
=== Log Analysis ===
Container ID: web-app
Total Lines: 50

Errors Found: 3
  - Error: Connection refused at line 15
  - Error: Timeout occurred at line 32
  - Error: Database unavailable at line 48

Warnings Found: 5
  - Warning: High memory usage detected
  - Warning: Deprecated API usage
  ...
```

### 4. `get_application_summary`
Получить полный саммари приложения в контейнере.

**Параметры:**
- `host` (string, required)
- `port` (integer, optional)
- `username` (string, required)
- `container_id` (string, required)

**Результат:**
```
=== Application Summary ===
Container: web-app
Image: myregistry/web-app:latest
Status: Up 2 hours
State: running

Recent Errors: 2
Recent Warnings: 1

Resource Stats:
CONTAINER           CPU %   MEM USAGE / LIMIT
web-app            0.15%   256.5MiB / 1GiB
```

### 5. `get_container_stats`
Получить статистику ресурсов контейнера (CPU, память и т.д.).

**Параметры:**
- `host` (string, required)
- `port` (integer, optional)
- `username` (string, required)
- `container_id` (string, required)

### 6. `inspect_container`
Детальная информация о конфигурации контейнера.

**Параметры:**
- `host` (string, required)
- `port` (integer, optional)
- `username` (string, required)
- `container_id` (string, required)

### 7. `search_containers`
Поиск контейнеров по имени или образу.

**Параметры:**
- `host` (string, required)
- `port` (integer, optional)
- `username` (string, required)
- `pattern` (string, required) - Паттерн поиска

## Примеры использования

### Проверка состояния приложения

```bash
# Список контейнеров
curl -X POST http://localhost:8765/list_docker_containers \
  -H "Content-Type: application/json" \
  -d '{
    "host": "production.server.com",
    "username": "admin",
    "port": 22
  }'

# Получить логи приложения
curl -X POST http://localhost:8765/get_container_logs \
  -H "Content-Type: application/json" \
  -d '{
    "host": "production.server.com",
    "username": "admin",
    "container_id": "api-service",
    "lines": 200
  }'

# Анализ ошибок
curl -X POST http://localhost:8765/analyze_container_logs \
  -H "Content-Type: application/json" \
  -d '{
    "host": "production.server.com",
    "username": "admin",
    "container_id": "api-service",
    "lines": 100
  }'
```

## SSH подключение

Сервер использует Apache MINA SSHD для SSH подключения. По умолчанию:
- Порт: 22
- Поддержка ED25519, RSA, ECDSA ключей
- Таймаут подключения: 30 секунд
- Таймаут команды: 60 секунд
- OpenSSH формат ключей

### Рекомендации безопасности

1. Используйте приватные ключи вместо паролей
2. Ограничьте доступ SSH ключа правильными permissions (600)
3. Используйте отдельного пользователя для MCP сервера с минимальными привилегиями
4. Добавьте SSH ключ в `authorized_keys` на удаленном сервере
5. Используйте ED25519 ключи вместо RSA для лучшей безопасности

## Логирование

Логирование настроено через Logback. Конфигурация в `application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.ai.advent.mcp.docker: DEBUG
```

Уровни:
- **INFO** - основная информация о работе
- **DEBUG** - детальная информация для отладки
- **ERROR** - ошибки

Логи выводятся в консоль и содержат timestamp и сообщение.

## Обработка ошибок

Сервер обрабатывает следующие типы ошибок:

1. **SSH ошибки** - проблемы с подключением к серверу
2. **Docker ошибки** - контейнер не существует, команда не доступна
3. **Парсинг ошибки** - некорректный JSON в ответе Docker
4. **Таймауты** - истечение времени на выполнение команды

Все ошибки логируются и возвращаются в ответе MCP с пометкой `isError: true`.

## Расширение функционала

### Добавление нового инструмента

1. Добавьте метод в `DockerService`
2. Создайте handler в `ToolsDefinitionService` для определения инструмента
3. Определите `ToolCallRequest` с параметрами в контроллере
4. Реализуйте обработчик в соответствующем контроллере

### Пример добавления нового инструмента:

**1. В DockerService:**
```java
public String getMyCustomInfo(String host, String username, int port) {
    SshManager sshManager = new SshManager(host, port, username);
    try {
        return sshManager.executeCommand("docker ps");
    } finally {
        sshManager.disconnect();
    }
}
```

**2. В ToolsDefinitionService:**
```java
public ToolDefinition defineMyCustomTool() {
    return new ToolDefinition(
        "my_custom_tool",
        "Description of my tool",
        new Map<>(Map.ofEntries(
            Map.entry("host", new Property("string", "The host to connect to")),
            Map.entry("username", new Property("string", "SSH username"))
        ))
    );
}
```

## Тестирование

```bash
# Запуск unit тестов
mvn test

# Запуск с coverage
mvn test jacoco:report
```

## Проблемы и решение

### Проблема: "No active SSH connection"
**Решение:** Убедитесь, что вы передали правильные host, port и username, и сервер доступен.

### Проблема: "Docker command not found"
**Решение:** Убедитесь, что Docker установлен на удаленном сервере и пользователь SSH имеет доступ к Docker.

### Проблема: Permission denied
**Решение:** Добавьте SSH пользователя в docker группу или используйте sudo.

```bash
sudo usermod -aG docker $USER
newgrp docker
```

## Развертывание

### Docker

```dockerfile
FROM eclipse-temurin:21-jdk-alpine
COPY target/mcp-docker-monitor-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mcp-docker-monitor
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mcp-docker-monitor
  template:
    metadata:
      labels:
        app: mcp-docker-monitor
    spec:
      containers:
      - name: mcp-docker-monitor
        image: myregistry/mcp-docker-monitor:1.0.0
        ports:
        - containerPort: 8765
```

## Лицензия

MIT

## Автор

AI Advent Challenge 2024

## Поддержка

Для проблем и вопросов создавайте issues в репозитории проекта.

