# Compare-Quality: Быстрый справочник

## Краткое описание

Endpoint `POST /api/search/compare-quality` сравнивает три режима фильтрации поиска:
- **Mode A**: Без фильтра
- **Mode B**: С пороговым фильтром (merged_score)
- **Mode C**: С LLM-фильтром (llmScore) - опционально

## Быстрые примеры

### 1. Базовый поиск (только Mode A и B)
```bash
curl -X POST "http://localhost:8080/api/search/compare-quality?query=python&topK=5&filterThreshold=0.5"
```

**Параметры:**
- `query=python` - поисковый запрос
- `topK=5` - количество результатов
- `filterThreshold=0.5` - порог для режима B

---

### 2. Поиск со всеми тремя режимами

```bash
curl -X POST "http://localhost:8080/api/search/compare-quality?query=machine%20learning&topK=10&filterThreshold=0.3&useLlmReranker=true&llmFilterThreshold=0.7"
```

**Параметры:**
- `useLlmReranker=true` - включить режим C
- `llmFilterThreshold=0.7` - порог для LLM-фильтра

---

### 3. Строгая фильтрация

```bash
curl -X POST "http://localhost:8080/api/search/compare-quality?query=quantum%20computing&topK=20&filterThreshold=0.8&useLlmReranker=true&llmFilterThreshold=0.9"
```

Удаляет много результатов, но оставляет только высокорелевантные.

---

### 4. Мягкая фильтрация

```bash
curl -X POST "http://localhost:8080/api/search/compare-quality?query=neural%20networks&topK=20&filterThreshold=0.2&useLlmReranker=true&llmFilterThreshold=0.5"
```

Удаляет мало результатов, сохраняя большинство.

---

### 5. С форматированием JSON (jq)

```bash
curl -s -X POST "http://localhost:8080/api/search/compare-quality?query=deep%20learning&topK=10&filterThreshold=0.5&useLlmReranker=true&llmFilterThreshold=0.7" \
  | jq '.'
```

---

## Интерпретация ответа

### Ключевые метрики Mode B (пороговый фильтр)

```json
{
  "countBefore": 10,                          // Всего результатов
  "countAfterThreshold": 7,                   // Осталось после фильтра
  "countRemovedThreshold": 3,                 // Удалено
  "percentageRemovedThreshold": 30.0,         // % удаленных
  "avgScoreBefore": 0.62,                     // Средний score ДО
  "avgScoreAfterThreshold": 0.71              // Средний score ПОСЛЕ
}
```

**Вывод:** Удалено 30% результатов, но средний score улучшился с 0.62 до 0.71.

---

### Ключевые метрики Mode C (LLM-фильтр)

```json
{
  "countAfterLlm": 8,                         // Осталось после LLM-фильтра
  "countRemovedLlm": 2,                       // Удалено LLM-фильтром
  "percentageRemovedLlm": 20.0,               // % удаленных
  "avgLlmScoreBefore": 0.58,                  // Средний llmScore ДО
  "avgLlmScoreAfter": 0.82,                   // Средний llmScore ПОСЛЕ
  "avgScoreAfterLlm": 0.73                    // Средний merged_score ПОСЛЕ LLM-фильтра
}
```

**Вывод:** LLM-фильтр удалил 20% результатов, LLM-оценка улучшилась с 0.58 до 0.82.

---

## Сравнение режимов

```
Режим A (No filter)
├── Results: 10
└── Avg Score: 0.62

Режим B (Threshold: 0.5)
├── Results: 7 (-30%)
├── Avg Score: 0.71 (+0.09)
└── Verdict: Хорошее соотношение качество-количество

Режим C (LLM: 0.7)
├── Results: 8 (-20%)
├── Avg LLM Score: 0.82 (+0.24)
├── Avg Score: 0.73 (+0.11)
└── Verdict: Лучше качество, чем Mode B
```

---

## Тестовые сценарии

### Сценарий 1: Русский язык, фрагменты про детей

```bash
curl -s -X POST "http://localhost:8080/api/search/compare-quality?query=истории%20про%20детей&topK=10&filterThreshold=0.3&useLlmReranker=true&llmFilterThreshold=0.7" | jq '.countBefore, .countAfterThreshold, .countAfterLlm, .percentageRemovedThreshold, .percentageRemovedLlm'
```

**Ожидаемый вывод:**
```
10
7
8
30.0
20.0
```

---

### Сценарий 2: Английский язык, технический запрос

```bash
curl -s -X POST "http://localhost:8080/api/search/compare-quality?query=REST%20API%20design%20patterns&topK=15&filterThreshold=0.6&useLlmReranker=true&llmFilterThreshold=0.75" | jq '.avgScoreBefore, .avgScoreAfterThreshold, .avgLlmScoreAfter'
```

**Ожидаемый вывод:**
```
0.65
0.74
0.79
```

---

## Параметры по умолчанию

| Параметр | Значение | Описание |
|----------|----------|----------|
| `query` | *required* | Поисковый запрос |
| `topK` | 5 | Сколько результатов вернуть |
| `filterThreshold` | 0.5 | Порог Mode B (merged_score) |
| `useLlmReranker` | false | Включить ли Mode C |
| `llmFilterThreshold` | 0.7 | Порог Mode C (llmScore) |

---

## Рекомендуемые значения пороговых фильтров

### Мягкая фильтрация (максимум результатов)
- `filterThreshold: 0.2`
- `llmFilterThreshold: 0.5`

### Сбалансированная (по умолчанию)
- `filterThreshold: 0.5`
- `llmFilterThreshold: 0.7`

### Строгая фильтрация (только лучшие результаты)
- `filterThreshold: 0.7`
- `llmFilterThreshold: 0.85`

### Очень строгая (максимум качество)
- `filterThreshold: 0.8`
- `llmFilterThreshold: 0.9`

---

## Понимание LLM-оценки

LLM-оценка вычисляется на основе:

1. **Совпадение ключевых слов** (60% веса)
   - Сколько слов из запроса есть в тексте
   - Пример: запрос "machine learning" → поиск слов "machine" и "learning"

2. **Длина текста** (20% веса)
   - Оптимально 300-1000 символов
   - Слишком короткие или слишком длинные снижают оценку

3. **Позиция совпадений** (20% веса)
   - Совпадения в начале текста ценнее
   - Это помогает найти релевантные введения

**Пример:**
```
Запрос: "Python data structures"
Текст: "Python list is a data structures in Python language..."

LLM-score = 0.6 * 1.0 + 0.2 * 0.9 + 0.2 * 0.95 = 0.97
```

---

## Проверка здоровья endpoint

```bash
# Простой запрос (должен вернуть 200)
curl -i -X POST "http://localhost:8080/api/search/compare-quality?query=test&topK=5"

# С полными параметрами
curl -i -X POST "http://localhost:8080/api/search/compare-quality?query=test&topK=5&filterThreshold=0.5&useLlmReranker=true&llmFilterThreshold=0.7"
```

**Проверяемое:**
- HTTP 200 OK
- Content-Type: application/json
- Все поля присутствуют в ответе

---

## Ошибки и их решение

### Ошибка: "Query is required"
```bash
# ❌ Неправильно
curl -X POST "http://localhost:8080/api/search/compare-quality"

# ✅ Правильно
curl -X POST "http://localhost:8080/api/search/compare-quality?query=test"
```

### Ошибка: "topK must be between 1 and 100"
```bash
# ❌ Неправильно
curl -X POST "http://localhost:8080/api/search/compare-quality?query=test&topK=0"

# ✅ Правильно
curl -X POST "http://localhost:8080/api/search/compare-quality?query=test&topK=10"
```

### Ошибка: "filterThreshold must be between 0.0 and 1.0"
```bash
# ❌ Неправильно
curl -X POST "http://localhost:8080/api/search/compare-quality?query=test&filterThreshold=1.5"

# ✅ Правильно
curl -X POST "http://localhost:8080/api/search/compare-quality?query=test&filterThreshold=0.5"
```

---

## Performance

- **Без LLM-фильтра**: ~50-100ms
- **С LLM-фильтром**: ~100-200ms
- Зависит от размера `topK` и длины текстов

Все операции выполняются синхронно, результаты возвращаются через JSON.

---

## Дальнейшие действия

1. **Оптимизация LLM-оценки**
   - Использовать реальные LLM API (OpenAI, Claude)
   - Добавить кэширование оценок

2. **Визуализация**
   - Создать UI для сравнения трёх режимов
   - Интерактивные графики метрик

3. **Экспорт результатов**
   - CSV экспорт для анализа в Excel
   - PDF отчёты

---

## Примеры интеграции в код

### Spring Boot Client

```java
@Service
public class SearchQualityService {
    @Autowired
    private RestTemplate restTemplate;
    
    public SearchQualityMetrics compareThreeModes(String query, int topK, double threshold, double llmThreshold) {
        String url = "http://localhost:8080/api/search/compare-quality" +
            "?query={query}&topK={topK}&filterThreshold={threshold}" +
            "&useLlmReranker=true&llmFilterThreshold={llmThreshold}";
        
        return restTemplate.getForObject(url, SearchQualityMetrics.class,
            query, topK, threshold, llmThreshold);
    }
}
```

### Python Client

```python
import requests

response = requests.post(
    'http://localhost:8080/api/search/compare-quality',
    params={
        'query': 'machine learning',
        'topK': 10,
        'filterThreshold': 0.5,
        'useLlmReranker': True,
        'llmFilterThreshold': 0.7
    }
)

metrics = response.json()
print(f"Mode A: {metrics['countBefore']} results")
print(f"Mode B: {metrics['countAfterThreshold']} results")
print(f"Mode C: {metrics['countAfterLlm']} results")
```

### JavaScript/Node.js Client

```javascript
const fetch = require('node-fetch');

const response = await fetch(
  'http://localhost:8080/api/search/compare-quality' +
  '?query=machine+learning&topK=10&filterThreshold=0.5' +
  '&useLlmReranker=true&llmFilterThreshold=0.7'
);

const metrics = await response.json();
console.log(`Mode A: ${metrics.countBefore} results`);
console.log(`Mode B: ${metrics.countAfterThreshold} results`);
console.log(`Mode C: ${metrics.countAfterLlm} results`);
```

---

## Контакт и поддержка

Для вопросов и отчётов об ошибках:
- GitHub Issues: [AI_Advent_Challenge/issues]
- Email: [support@example.com]

---

**Последнее обновление:** 2025-12-24

