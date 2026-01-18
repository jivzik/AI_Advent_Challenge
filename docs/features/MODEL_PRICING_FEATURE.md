# 💰 Расчет стоимости API запросов - Model Pricing Configuration

## 📋 Обзор

Реализована система расчета стоимости API запросов для платных моделей в OpenRouter. Коэффициенты цен захардкодены в конфиге для каждой модели, что позволяет точно рассчитывать затраты на основе количества используемых токенов.

## 🎯 Ключевые компоненты

### 1. **ModelPricingConfig** - Конфиг с ценами моделей
**Файл:** `config/ModelPricingConfig.java`

```java
public class ModelPricingConfig {
    // Цены за 1 миллион токенов (1M) для каждой модели
    PRICING_MAP.put("anthropic/claude-3.5-sonnet", new ModelPricing(3.00, 15.00));
    // inputPrice: 3.00 $/1M, outputPrice: 15.00 $/1M
}
```

**Поддерживаемые модели:**
- Anthropic Claude (opus, 3.5-sonnet, 3-sonnet, 3-haiku)
- OpenAI (GPT-4, GPT-4o, GPT-3.5-turbo)
- Google Gemini (gemma-3n-e4b-it, gemini-pro, gemini-1.5-pro)
- Mistral (large, medium, small-24b)
- Meta Llama (llama-3-70b, llama-2-70b)
- Perplexity (pplx-7b-online, pplx-70b-online, pplx-70b-chat)

### 2. **CostCalculationService** - Сервис расчета стоимости
**Файл:** `service/CostCalculationService.java`

#### Метод: `calculateCost(String modelName, int inputTokens, int outputTokens)`
Рассчитывает стоимость на основе количества токенов:
```java
CostBreakdown costBreakdown = costCalculationService.calculateCost(
    "anthropic/claude-3.5-sonnet", 
    150,    // inputTokens
    250     // outputTokens
);

// Результат:
// costInput = 150 * 3.00 / 1_000_000 = 0.00045
// costOutput = 250 * 15.00 / 1_000_000 = 0.00375
// totalCost = 0.0042
```

#### Метод: `calculateCostFromTotal(String modelName, double totalCost)`
Использует предоставленную от API общую стоимость:
```java
CostBreakdown costBreakdown = costCalculationService.calculateCostFromTotal(
    "anthropic/claude-3.5-sonnet",
    0.00234  // totalCost from API response
);
```

### 3. **CostBreakdown** - Детализированная информация о стоимости
Внутренний класс CostCalculationService:
```java
public static class CostBreakdown {
    - modelName: String              // Имя модели
    - inputTokens: int               // Количество входящих токенов
    - outputTokens: int              // Количество выходящих токенов
    - inputPricePerMillion: double   // Цена за 1M входящих токенов
    - outputPricePerMillion: double  // Цена за 1M выходящих токенов
    - inputCost: double              // Стоимость входа
    - outputCost: double             // Стоимость выхода
    - totalCost: double              // Общая стоимость
    
    // Метод для красивого вывода логов
    getFormattedString(): String
}
```

## 🔧 Интеграция с OpenRouterToolClient

OpenRouterToolClient автоматически использует CostCalculationService для расчета стоимости:

```java
@Component
public class OpenRouterToolClient {
    
    private final CostCalculationService costCalculationService;
    
    // В методе executeRequest():
    if (response.getUsage() != null) {
        Integer promptTokens = response.getUsage().getPromptTokens();
        Integer completionTokens = response.getUsage().getCompletionTokens();
        
        // Рассчитываем стоимость по сконфигурированным ценам
        CostCalculationService.CostBreakdown costBreakdown =
            costCalculationService.calculateCost(
                response.getModel(),
                promptTokens,
                completionTokens
            );
        
        if (costBreakdown != null) {
            log.info("💵 Calculated cost: {}", costBreakdown.getFormattedString());
        }
    }
}
```

## 📊 Формулы расчета

```
costInput = inputTokens * priceInputPerMillion / 1_000_000
costOutput = outputTokens * priceOutputPerMillion / 1_000_000
totalCost = costInput + costOutput
```

**Пример расчета для Claude 3.5 Sonnet:**
- Входящие токены: 100
- Выходящие токены: 200
- Цена за 1M входящих: $3.00
- Цена за 1M выходящих: $15.00

```
costInput = 100 * 3.00 / 1_000_000 = $0.0003
costOutput = 200 * 15.00 / 1_000_000 = $0.003
totalCost = $0.0033
```

## 📝 Логирование

Сервис логирует информацию о стоимости в формате:

```
💰 Tokens - Input: 150, Output: 250, Total: 400
💵 Cost from API: 0.00234
💵 Calculated cost: 💰 Tokens: Input=150 (priced at $3.00/1M), Output=250 (priced at $15.00/1M) | 💵 Costs: Input=$0.000450, Output=$0.003750, Total=$0.004200
```

## ✅ Статус компиляции

```
BUILD SUCCESS ✓
Total time: 2.995 s
```

## 🚀 Использование в приложении

1. **Автоматическое вычисление:** CostCalculationService интегрирован в OpenRouterToolClient и вычисляет стоимость автоматически при каждом запросе к API.

2. **Обработка неизвестных моделей:** Если модель не найдена в конфиге, сервис логирует предупреждение:
   ```
   ⚠️ Unable to calculate cost - pricing not configured for model: unknown-model
   ```

3. **Гибкость:** Легко добавить новую модель, просто добавив её в `PRICING_MAP` в `ModelPricingConfig`.

## 📚 Добавление новой модели

Для добавления новой модели отредактируйте `ModelPricingConfig.java`:

```java
static {
    // Добавить в PRICING_MAP
    PRICING_MAP.put("provider/new-model", new ModelPricing(inputPrice, outputPrice));
    // inputPrice и outputPrice - цены за 1 миллион токенов
}
```

## 📁 Файлы проекта

| Файл | Назначение |
|------|-----------|
| `config/ModelPricingConfig.java` | Конфиг цен для моделей |
| `service/CostCalculationService.java` | Сервис расчета стоимости |
| `service/openrouter/OpenRouterToolClient.java` | Интеграция с API (обновлено) |

## 🎉 Результат

✅ Полностью функциональная система расчета стоимости API запросов  
✅ Захардкодированные коэффициенты цен для популярных моделей  
✅ Автоматический расчет при каждом запросе к OpenRouter API  
✅ Подробное логирование с информацией о стоимости  
✅ Код скомпилирован без ошибок  

