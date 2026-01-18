# Руководство по фильтрации релевантности и сравнению качества поиска

## Обзор

Этот модуль добавляет функциональность для фильтрации результатов поиска по релевантности и сравнения качества поиска с фильтром и без фильтра.

### Архитектура (Clean Code + SOLID)

#### **Single Responsibility Principle (SRP)**
- `RelevanceFilter` - интерфейс для фильтрации
- `ThresholdRelevanceFilter` - фильтрует по пороговому значению
- `NoopRelevanceFilter` - нейтральный фильтр (без фильтрации)
- `RelevanceFilteringService` - управление фильтрами
- `SearchQualityComparator` - сравнение результатов и вычисление метрик

#### **Open/Closed Principle**
Можно добавлять новые фильтры, не модифицируя существующий код:
```java
public class CustomRelevanceFilter implements RelevanceFilter {
    @Override
    public List<MergedSearchResultDto> filter(List<MergedSearchResultDto> results) {
        // Ваша логика фильтрации
    }
}
```

#### **Dependency Inversion**
Зависимости от интерфейсов:
```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final RelevanceFilter filter;  // Зависимость от интерфейса
}
```

---

## Компоненты

### 1. **RelevanceFilter** (интерфейс)
**Путь:** `de.jivz.rag.service.filtering.RelevanceFilter`

```java
public interface RelevanceFilter {
    List<MergedSearchResultDto> filter(List<MergedSearchResultDto> results);
    String getName();
    String getDescription();
}
```

**Методы:**
- `filter()` - применить фильтр к результатам
- `getName()` - название фильтра (для логирования)
- `getDescription()` - описание конфигурации

---

### 2. **ThresholdRelevanceFilter**
**Путь:** `de.jivz.rag.service.filtering.ThresholdRelevanceFilter`

Фильтрует результаты по минимальному пороговому значению merged_score.

**Пример:**
```java
RelevanceFilter filter = new ThresholdRelevanceFilter(0.6);
List<MergedSearchResultDto> filtered = filter.filter(results);
```

**Логика:**
- Проходят только результаты, где `merged_score >= threshold`
- Логирует количество отфильтрованных результатов

---

### 3. **NoopRelevanceFilter**
**Путь:** `de.jivz.rag.service.filtering.NoopRelevanceFilter`

Нейтральный фильтр (Null Object паттерн), возвращает все результаты без изменений.

**Использование:**
```java
RelevanceFilter filter = new NoopRelevanceFilter();
List<MergedSearchResultDto> filtered = filter.filter(results);  // Возвращает все результаты
```

---

### 4. **RelevanceFilteringService**
**Путь:** `de.jivz.rag.service.RelevanceFilteringService`

Сервис для управления фильтрами (Factory паттерн).

**Методы:**

```java
// Создание фильтра по типу и конфигурации
RelevanceFilter filter = filteringService.createFilter(
    FilterType.THRESHOLD, 
    0.5
);

// Применение фильтра
List<MergedSearchResultDto> filtered = filteringService.applyFilter(results, filter);

// Удобные методы
List<MergedSearchResultDto> filtered = filteringService.applyThresholdFilter(results, 0.6);
```

**Типы фильтров:**
- `FilterType.THRESHOLD` - фильтр по пороговому значению
- `FilterType.NOOP` - без фильтрации

---

### 5. **SearchQualityComparator**
**Путь:** `de.jivz.rag.service.SearchQualityComparator`

Сервис для сравнения качества поиска с фильтром и без фильтра.

**Методы:**

```java
// Сравнение с заданным фильтром
SearchQualityMetrics metrics = comparator.compareWithAndWithoutFilter(
    results,
    filter,
    query
);

// Сравнение с пороговым фильтром
SearchQualityMetrics metrics = comparator.compareWithThresholdFilter(
    results,
    FilterType.THRESHOLD,
    0.6,
    query
);
```

**Вычисляемые метрики:**
- `precision` - доля оставшихся результатов
- `recall` - доля сохранённых результатов
- `f1Score` - гармоническое среднее precision и recall
- `avgScoreBefore/After` - средний score до и после фильтра
- `countRemoved` - количество отфильтрованных результатов
- `percentageRemoved` - процент отфильтрованных результатов

---

### 6. **SearchQualityMetrics** (DTO)
**Путь:** `de.jivz.rag.dto.SearchQualityMetrics`

Результаты сравнения качества поиска.

**Основные поля:**
```java
SearchQualityMetrics metrics = SearchQualityMetrics.builder()
    .query("machine learning")
    .filterName("ThresholdRelevanceFilter_0.6")
    .countBefore(10)
    .countAfter(7)
    .countRemoved(3)
    .percentageRemoved(30.0)
    .precision(0.7)
    .recall(0.7)
    .f1Score(0.7)
    .avgScoreBefore(0.65)
    .avgScoreAfter(0.72)
    .build();
```

---

## REST API

### Endpoint 1: Сравнение качества поиска

**POST** `/api/search/compare-quality`

**Query Parameters:**
- `query` (required) - поисковый запрос
- `topK` (optional, default: 5) - количество результатов
- `filterThreshold` (optional, default: 0.5) - порог фильтра релевантности

**Пример запроса:**
```bash
curl -X POST "http://localhost:8080/api/search/compare-quality?query=machine+learning&topK=10&filterThreshold=0.6"
```

**Пример ответа:**
```json
{
  "query": "machine learning",
  "filterName": "ThresholdRelevanceFilter_0.6",
  "filterDescription": "Filters results with merged_score < 0.6",
  "countBefore": 10,
  "countAfter": 7,
  "countRemoved": 3,
  "percentageRemoved": 30.0,
  "precision": 0.7,
  "recall": 0.7,
  "f1Score": 0.7,
  "avgScoreBefore": 0.65,
  "avgScoreAfter": 0.72,
  "minScoreBefore": 0.42,
  "maxScoreBefore": 0.89,
  "minScoreAfter": 0.61,
  "maxScoreAfter": 0.89,
  "executionTimeMs": 125,
  "filterApplied": true,
  "comment": "Отфильтровано 3 результатов. Средний score 0.6500 → 0.7200 (разница: 0.0700)",
  "resultsWithoutFilter": [...],
  "resultsWithFilter": [...]
}
```

---

### Endpoint 2: Поиск с фильтром релевантности

**POST** `/api/search/with-filter`

**Request Body:**
```json
{
  "query": "machine learning",
  "topK": 5,
  "searchMode": "hybrid",
  "semanticWeight": 0.6,
  "threshold": 0.5,
  "applyRelevanceFilter": true,
  "relevanceFilterType": "THRESHOLD",
  "relevanceFilterThreshold": 0.6
}
```

**Параметры фильтрации:**
- `applyRelevanceFilter` (boolean) - применять ли фильтр релевантности
- `relevanceFilterType` (string) - тип фильтра ("THRESHOLD" или "NOOP")
- `relevanceFilterThreshold` (double) - порог фильтра (0.0-1.0)

**Пример ответа:** SearchResponseDto с отфильтрованными результатами

---

## Примеры использования

### Пример 1: Простая фильтрация по порогу

```java
@Service
@RequiredArgsConstructor
public class MySearchService {
    private final RelevanceFilteringService filteringService;
    
    public List<MergedSearchResultDto> searchWithFilter(
            List<MergedSearchResultDto> results,
            double threshold) {
        
        return filteringService.applyThresholdFilter(results, threshold);
    }
}
```

### Пример 2: Сравнение качества поиска

```java
@Service
@RequiredArgsConstructor
public class MySearchAnalyzer {
    private final HybridSearchService hybridSearchService;
    private final RelevanceFilteringService filteringService;
    private final SearchQualityComparator comparator;
    
    public SearchQualityMetrics analyzeFilterImpact(String query, double threshold) {
        // Выполняем гибридный поиск
        List<MergedSearchResultDto> results = hybridSearchService.search(
            query, 10, 0.0, 0.6, 0.4
        );
        
        // Создаём фильтр
        var filter = filteringService.createThresholdFilter(threshold);
        
        // Сравниваем результаты
        return comparator.compareWithAndWithoutFilter(results, filter, query);
    }
}
```

### Пример 3: Использование в контроллере

```java
@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchQualityComparator comparator;
    
    @PostMapping("/compare-quality")
    public ResponseEntity<?> compare(
            @RequestParam String query,
            @RequestParam(defaultValue = "0.5") double threshold) {
        
        // Получаем результаты и метрики
        SearchQualityMetrics metrics = // ... вычисляем ...
        
        return ResponseEntity.ok(metrics);
    }
}
```

---

## Best Practices

### 1. **Используйте интерфейс RelevanceFilter**
```java
// ✅ Хорошо - зависит от интерфейса
private RelevanceFilter filter;

// ❌ Плохо - зависит от конкретного класса
private ThresholdRelevanceFilter filter;
```

### 2. **Создавайте фильтры через RelevanceFilteringService**
```java
// ✅ Хорошо - используем factory
RelevanceFilter filter = filteringService.createThresholdFilter(0.6);

// ❌ Плохо - прямая инстанциация
RelevanceFilter filter = new ThresholdRelevanceFilter(0.6);
```

### 3. **Логирование метрик качества**
```java
SearchQualityMetrics metrics = comparator.compareWithAndWithoutFilter(results, filter, query);
log.info("Metrics: {}", metrics);  // Красивое форматирование через toString()
```

### 4. **Обработка ошибок**
```java
if (query == null || query.isBlank()) {
    throw new IllegalArgumentException("Query cannot be empty");
}

if (threshold < 0.0 || threshold > 1.0) {
    throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
}
```

---

## Тестирование

### Unit тест для фильтра

```java
@Test
public void testThresholdFilter() {
    // Arrange
    RelevanceFilter filter = new ThresholdRelevanceFilter(0.6);
    List<MergedSearchResultDto> results = Arrays.asList(
        MergedSearchResultDto.builder().mergedScore(0.8).build(),
        MergedSearchResultDto.builder().mergedScore(0.5).build(),
        MergedSearchResultDto.builder().mergedScore(0.7).build()
    );
    
    // Act
    List<MergedSearchResultDto> filtered = filter.filter(results);
    
    // Assert
    assertEquals(2, filtered.size());  // Только 0.8 и 0.7 проходят
}
```

### Integration тест для сравнения качества

```java
@SpringBootTest
public class SearchQualityTest {
    @Autowired
    private HybridSearchService hybridSearchService;
    
    @Autowired
    private SearchQualityComparator comparator;
    
    @Test
    public void testCompareQuality() {
        // Arrange
        String query = "test query";
        
        // Act
        List<MergedSearchResultDto> results = hybridSearchService.search(query, 10, 0.0, 0.6, 0.4);
        var filter = new ThresholdRelevanceFilter(0.6);
        SearchQualityMetrics metrics = comparator.compareWithAndWithoutFilter(results, filter, query);
        
        // Assert
        assertTrue(metrics.getCountBefore() >= metrics.getCountAfter());
        assertEquals(metrics.getCountBefore() - metrics.getCountAfter(), metrics.getCountRemoved());
    }
}
```

---

## FAQ

### Q: Когда использовать фильтр релевантности?
**A:** Используйте фильтр, когда:
- Результаты содержат много низкокачественных совпадений
- Нужна высокая точность (precision) за счет полноты (recall)
- Пользователи жалуются на релевантность результатов

### Q: Какой порог выбрать?
**A:** Рекомендуемые значения:
- `0.3 - 0.4` - мягкая фильтрация (низкая точность, высокая полнота)
- `0.5 - 0.6` - средняя фильтрация (баланс)
- `0.7 - 0.8` - жёсткая фильтрация (высокая точность, низкая полнота)

### Q: Как добавить новый тип фильтра?
**A:** 
1. Создайте класс, реализующий `RelevanceFilter`
2. Добавьте тип в enum `FilterType` в `RelevanceFilteringService`
3. Добавьте case в switch методе `createFilter()`

### Q: Можно ли комбинировать несколько фильтров?
**A:** Да, можно создать `CompositeRelevanceFilter` с цепочкой фильтров (планируется в будущей версии).

---

## Производительность

### Временная сложность
- **Фильтрация:** O(n) - один проход по результатам
- **Сравнение:** O(n) - один проход для вычисления метрик
- **Общее:** O(n) - линейная сложность

### Рекомендации
- Для больших наборов данных (>10000 результатов) рекомендуется применять фильтр ДО финализации
- Используйте кеширование результатов сравнения для одних и тех же query+filter комбинаций

---

## Изменения в существующих классах

### SearchRequest
Добавлены новые поля для конфигурации фильтра:
```java
@Builder.Default
private Boolean applyRelevanceFilter = false;

@Builder.Default
private String relevanceFilterType = "THRESHOLD";

@Builder.Default
private Double relevanceFilterThreshold = 0.5;
```

### HybridSearchPipelineService
Добавлен параметр `relevanceFilter` в `PipelineConfig`:
```java
RelevanceFilter relevanceFilter;
```

Пайплайн теперь включает этап фильтрации между переранжированием и финализацией.

---

## Ссылки

- **RelevanceFilter:** `de.jivz.rag.service.filtering.RelevanceFilter`
- **RelevanceFilteringService:** `de.jivz.rag.service.RelevanceFilteringService`
- **SearchQualityComparator:** `de.jivz.rag.service.SearchQualityComparator`
- **SearchQualityMetrics:** `de.jivz.rag.dto.SearchQualityMetrics`
- **SearchController:** `de.jivz.rag.controller.SearchController`

