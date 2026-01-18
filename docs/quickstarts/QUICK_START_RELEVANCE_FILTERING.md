# Краткая справка по фильтрации релевантности

## ⚡ Быстрый старт (5 минут)

### 1️⃣ Сравнить качество поиска через REST API

```bash
# Сравнить результаты с фильтром (0.6) и без
curl -X POST "http://localhost:8080/api/search/compare-quality?query=machine+learning&topK=10&filterThreshold=0.6" \
  -H "Content-Type: application/json"
```

**Ответ включает:**
- Количество результатов до/после фильтра
- Метрики качества (precision, recall, F1)
- Разницу в среднем score
- Время выполнения

---

### 2️⃣ Использовать фильтр в поиске

```bash
curl -X POST "http://localhost:8080/api/search/with-filter" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "machine learning",
    "topK": 5,
    "searchMode": "hybrid",
    "applyRelevanceFilter": true,
    "relevanceFilterThreshold": 0.6
  }'
```

---

### 3️⃣ Программное использование в Java

```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final RelevanceFilteringService filteringService;
    private final SearchQualityComparator comparator;
    private final HybridSearchService hybridSearch;
    
    // Применить фильтр к результатам
    public List<MergedSearchResultDto> filterResults(
            List<MergedSearchResultDto> results) {
        return filteringService.applyThresholdFilter(results, 0.6);
    }
    
    // Сравнить качество поиска
    public SearchQualityMetrics compareQuality(String query) {
        List<MergedSearchResultDto> results = hybridSearch.search(
            query, 10, 0.0, 0.6, 0.4
        );
        
        var filter = filteringService.createThresholdFilter(0.6);
        return comparator.compareWithAndWithoutFilter(results, filter, query);
    }
}
```

---

## 📊 Интерпретация метрик

| Метрика | Диапазон | Что это значит |
|---------|----------|---|
| `precision` | 0.0 - 1.0 | Доля оставшихся результатов. 0.7 = 70% результатов сохранено |
| `recall` | 0.0 - 1.0 | Полнота. 0.7 = 70% исходных результатов осталось |
| `f1Score` | 0.0 - 1.0 | Баланс precision и recall. Чем выше, тем лучше |
| `avgScoreDiff` | ±∞ | Разница среднего score. +0.07 = улучшилось на 7% |
| `percentageRemoved` | 0.0 - 100.0 | % отфильтрованных результатов |

---

## 🎯 Рекомендуемые пороги

```
Threshold  | Использование | Примечание
-----------|--------------|----------
0.0 - 0.3  | Не фильтровать | Все результаты релевантны
0.3 - 0.5  | Мягкая         | Для broad поиска, низкий threshold
0.5 - 0.7  | Средняя        | Рекомендуется для большинства случаев ⭐
0.7 - 0.9  | Жёсткая        | Для high-precision поиска
0.9 - 1.0  | Очень жёсткая  | Только лучшие результаты
```

---

## 🔧 Параметры SearchRequest

```json
{
  "query": "что искать",
  "topK": 5,
  "threshold": 0.5,
  "searchMode": "hybrid",
  "semanticWeight": 0.6,
  
  // Параметры фильтрации релевантности
  "applyRelevanceFilter": true,
  "relevanceFilterType": "THRESHOLD",
  "relevanceFilterThreshold": 0.6
}
```

---

## 📈 Примеры результатов сравнения

### Пример 1: Мягкая фильтрация (threshold 0.3)

```json
{
  "countBefore": 10,
  "countAfter": 9,
  "countRemoved": 1,
  "percentageRemoved": 10.0,
  "precision": 0.9,
  "recall": 0.9,
  "f1Score": 0.9,
  "avgScoreBefore": 0.65,
  "avgScoreAfter": 0.66,
  "comment": "Отфильтровано 1 результат. Средний score 0.6500 → 0.6600 (разница: 0.0100)"
}
```
➜ **Мало удаляет, слабо влияет на качество**

### Пример 2: Средняя фильтрация (threshold 0.6)

```json
{
  "countBefore": 10,
  "countAfter": 7,
  "countRemoved": 3,
  "percentageRemoved": 30.0,
  "precision": 0.7,
  "recall": 0.7,
  "f1Score": 0.7,
  "avgScoreBefore": 0.65,
  "avgScoreAfter": 0.72,
  "comment": "Отфильтровано 3 результата. Средний score 0.6500 → 0.7200 (разница: 0.0700)"
}
```
➜ **Хороший баланс: удаляет плохие результаты, улучшает средний score**

### Пример 3: Жёсткая фильтрация (threshold 0.8)

```json
{
  "countBefore": 10,
  "countAfter": 2,
  "countRemoved": 8,
  "percentageRemoved": 80.0,
  "precision": 0.2,
  "recall": 0.2,
  "f1Score": 0.2,
  "avgScoreBefore": 0.65,
  "avgScoreAfter": 0.85,
  "comment": "Отфильтровано 8 результатов. Средний score 0.6500 → 0.8500 (разница: 0.2000)"
}
```
➜ **Очень жёсткая фильтрация: только лучшие результаты**

---

## 🏗️ Архитектура компонентов

```
SearchController
    ├── /api/search/compare-quality (GET)
    │   └── SearchQualityComparator.compareWithAndWithoutFilter()
    │       ├── HybridSearchService.search()
    │       ├── RelevanceFilter.filter()
    │       └── Вычисление метрик
    │
    └── /api/search/with-filter (POST)
        └── SearchRequestService.search()
            └── RelevanceFilteringService.applyFilter()
```

---

## 💡 Практические примеры

### Пример 1: Найти оптимальный порог для вашего датасета

```java
@Service
public class ThresholdOptimizer {
    @Autowired private SearchQualityComparator comparator;
    @Autowired private HybridSearchService search;
    
    public void findOptimalThreshold(String query) {
        var results = search.search(query, 20, 0.0, 0.6, 0.4);
        
        double[] thresholds = {0.3, 0.4, 0.5, 0.6, 0.7, 0.8};
        
        for (double threshold : thresholds) {
            var filter = new ThresholdRelevanceFilter(threshold);
            var metrics = comparator.compareWithAndWithoutFilter(
                results, filter, query
            );
            
            System.out.printf(
                "Threshold: %.1f | Removed: %d | F1: %.3f | Avg Score: %.3f%n",
                threshold,
                metrics.getCountRemoved(),
                metrics.getF1Score(),
                metrics.getAvgScoreAfter()
            );
        }
    }
}
```

### Пример 2: Динамическая фильтрация на основе метрик

```java
@Service
public class AdaptiveFilter {
    public List<MergedSearchResultDto> filterAdaptively(
            List<MergedSearchResultDto> results) {
        double avgScore = results.stream()
            .mapToDouble(r -> r.getMergedScore() != null ? r.getMergedScore() : 0)
            .average()
            .orElse(0.5);
        
        // Используем среднее значение как порог
        var filter = new ThresholdRelevanceFilter(avgScore);
        return filter.filter(results);
    }
}
```

### Пример 3: A/B тест фильтров

```java
@Service
public class ABTesting {
    public ABTestResult compareFilters(String query) {
        var results = search.search(query, 20, 0.0, 0.6, 0.4);
        
        var filterA = new ThresholdRelevanceFilter(0.5);
        var filterB = new ThresholdRelevanceFilter(0.6);
        
        var metricsA = comparator.compareWithAndWithoutFilter(results, filterA, query);
        var metricsB = comparator.compareWithAndWithoutFilter(results, filterB, query);
        
        return ABTestResult.builder()
            .filterA(metricsA)
            .filterB(metricsB)
            .winner(metricsA.getF1Score() > metricsB.getF1Score() ? "A" : "B")
            .build();
    }
}
```

---

## ❌ Частые ошибки

### ❌ Неправильно: Создание фильтра напрямую

```java
// Плохо - нарушает Open/Closed принцип
RelevanceFilter filter = new ThresholdRelevanceFilter(0.6);
```

### ✅ Правильно: Использование фабрики

```java
// Хорошо - зависит от интерфейса
RelevanceFilter filter = filteringService.createThresholdFilter(0.6);
```

---

### ❌ Неправильно: Жёстко кодировать пороги

```java
// Плохо - сложно менять
List<MergedSearchResultDto> filtered = results.stream()
    .filter(r -> r.getMergedScore() >= 0.5)
    .collect(Collectors.toList());
```

### ✅ Правильно: Параметризованный поиск

```java
// Хорошо - легко менять
SearchRequest request = SearchRequest.builder()
    .query(query)
    .applyRelevanceFilter(true)
    .relevanceFilterThreshold(0.5)
    .build();
searchService.search(request);
```

---

## 📚 Дополнительные ресурсы

- [RELEVANCE_FILTERING_GUIDE.md](./RELEVANCE_FILTERING_GUIDE.md) - подробное руководство
- Исходный код: `de.jivz.rag.service.filtering.*`
- Тесты: `src/test/java/de/jivz/rag/service/filtering/*`

---

## 🆘 Поиск и устранение неполадок

### Проблема: Фильтр удаляет слишком много результатов

**Решение:** Снизьте порог
```java
// Было
filteringService.applyThresholdFilter(results, 0.8);

// Стало
filteringService.applyThresholdFilter(results, 0.5);
```

### Проблема: Фильтр не влияет на результаты

**Решение:** Проверьте, что scores > 0
```java
double avgScore = results.stream()
    .mapToDouble(r -> r.getMergedScore() != null ? r.getMergedScore() : 0)
    .average()
    .orElse(0);

log.info("Average score: {}", avgScore);  // Должно быть > 0
```

### Проблема: SearchQualityMetrics возвращает null

**Решение:** Убедитесь, что results не пусто
```java
if (results == null || results.isEmpty()) {
    throw new IllegalArgumentException("Results cannot be empty");
}
```

---

## 📞 Контакт

Если у вас есть вопросы или предложения, создайте Issue в проекте.

