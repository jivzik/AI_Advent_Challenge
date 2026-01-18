# 💰 Model Pricing - Быстрый старт

## 🎯 Что было реализовано

Система автоматического расчета стоимости API запросов с захардкодированными ценами для платных моделей.

## 📊 Формула расчета

```
costInput = inputTokens * priceInputPerMillion / 1_000_000
costOutput = outputTokens * priceOutputPerMillion / 1_000_000
totalCost = costInput + costOutput
```

## 🔧 Компоненты

### 1. ModelPricingConfig
**Путь:** `backend/perplexity-service/src/main/java/de/jivz/ai_challenge/config/ModelPricingConfig.java`

Содержит захардкодированные цены за 1M токенов для каждой модели:
```java
PRICING_MAP.put("anthropic/claude-3.5-sonnet", new ModelPricing(3.00, 15.00));
//                                                           input  output
```

Поддерживаемые модели:
- ✅ Anthropic Claude (opus, 3.5-sonnet, 3-sonnet, 3-haiku)
- ✅ OpenAI (GPT-4, GPT-4o, GPT-3.5-turbo)
- ✅ Google Gemini
- ✅ Mistral
- ✅ Meta Llama
- ✅ Perplexity

### 2. CostCalculationService
**Путь:** `backend/perplexity-service/src/main/java/de/jivz/ai_challenge/service/CostCalculationService.java`

Основной сервис для расчета стоимости:

```java
// Использование
CostCalculationService.CostBreakdown costBreakdown =
    costCalculationService.calculateCost(
        "anthropic/claude-3.5-sonnet",
        150,    // inputTokens
        250     // outputTokens
    );

// Результат содержит:
// - modelName
// - inputTokens, outputTokens
// - inputCost, outputCost, totalCost
// - getFormattedString() для логирования
```

### 3. OpenRouterToolClient (обновлено)
**Путь:** `backend/perplexity-service/src/main/java/de/jivz/ai_challenge/service/openrouter/OpenRouterToolClient.java`

Интегрирован CostCalculationService для автоматического расчета при каждом запросе:

```java
// Автоматически вычисляет стоимость в методе executeRequest()
CostCalculationService.CostBreakdown costBreakdown =
    costCalculationService.calculateCost(modelUsed, promptTokens, completionTokens);

if (costBreakdown != null) {
    log.info("💵 Calculated cost: {}", costBreakdown.getFormattedString());
}
```

## 📝 Пример логов при запросе

```
💰 Tokens - Input: 150, Output: 250, Total: 400
💵 Cost from API: 0.00234
💵 Calculated cost: 💰 Tokens: Input=150 (priced at $3.00/1M), Output=250 (priced at $15.00/1M) | 💵 Costs: Input=$0.000450, Output=$0.003750, Total=$0.004200
```

## 🚀 Как использовать

### 1. Компиляция проекта
```bash
cd backend/perplexity-service
mvn clean compile
```

### 2. Запуск бэкенда
```bash
mvn spring-boot:run
```

### 3. Отправка запроса
```bash
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello, world!",
    "provider": "openrouter",
    "temperature": 0.7
  }'
```

### 4. Проверка логов
В консоли будут видны строки вида:
```
💵 Calculated cost: 💰 Tokens: Input=... (priced at $X.XX/1M), Output=... (priced at $X.XX/1M) | 💵 Costs: Input=$0.XXXXXX, Output=$0.XXXXXX, Total=$0.XXXXXX
```

## ➕ Добавление новой модели

Отредактируйте `ModelPricingConfig.java`:

```java
static {
    // Добавить строку в PRICING_MAP
    PRICING_MAP.put("provider/model-name", new ModelPricing(inputPrice, outputPrice));
    // inputPrice и outputPrice - цены за 1 миллион токенов
}
```

Затем пересоберите проект:
```bash
mvn clean compile
```

## ✅ Статус

- ✅ ModelPricingConfig создан и настроен
- ✅ CostCalculationService реализован
- ✅ OpenRouterToolClient интегрирован с расчетом стоимости
- ✅ Проект скомпилирован успешно
- ✅ Все логирование работает корректно

## 📚 Дополнительно

**Полная документация:** см. `MODEL_PRICING_FEATURE.md`

**Ключевые преимущества:**
- 💰 Точный расчет стоимости на основе реальных цен моделей
- 🔧 Простое добавление новых моделей
- 📝 Подробное логирование всех расчетов
- 🎯 Автоматическая интеграция без изменения API

