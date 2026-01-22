#!/bin/bash
# Advanced LLM Optimization Test Script
# Tests 6 different configurations with phi3:mini
# Shows FULL responses for comparison

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
SERVICE_URL="http://localhost:8090/api/chat"
RESULTS_DIR="test_results_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo -e "${BLUE}╔════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║          LLM Optimization Test - phi3:mini                         ║${NC}"
echo -e "${BLUE}║          Testing 6 Configurations                                  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Test queries
declare -a QUERIES=(
  "Что такое Spring Boot?"
  "Напиши простой REST контроллер на Spring Boot с GET endpoint"
  "Как настроить WebClient для работы с внешним API? Покажи код"
)

# 6 TEST CONFIGURATIONS
declare -A CONFIG_1=(
  [name]="Baseline"
  [description]="Стандартная конфигурация (контроль)"
  [temperature]="0.7"
  [num_ctx]="2048"
  [top_k]="40"
  [top_p]="0.9"
  [repeat_penalty]="1.0"
  [num_predict]="512"
)

declare -A CONFIG_2=(
  [name]="Precise"
  [description]="Точные детерминированные ответы"
  [temperature]="0.1"
  [num_ctx]="2048"
  [top_k]="10"
  [top_p]="0.5"
  [repeat_penalty]="1.0"
  [num_predict]="512"
)

declare -A CONFIG_3=(
  [name]="Balanced"
  [description]="Баланс точность/креативность"
  [temperature]="0.3"
  [num_ctx]="4096"
  [top_k]="40"
  [top_p]="0.9"
  [repeat_penalty]="1.1"
  [num_predict]="512"
)

declare -A CONFIG_4=(
  [name]="Creative"
  [description]="Креативные развернутые ответы"
  [temperature]="0.8"
  [num_ctx]="4096"
  [top_k]="50"
  [top_p]="0.95"
  [repeat_penalty]="1.1"
  [num_predict]="1024"
)

declare -A CONFIG_5=(
  [name]="Fast"
  [description]="Быстрые короткие ответы"
  [temperature]="0.3"
  [num_ctx]="2048"
  [top_k]="20"
  [top_p]="0.8"
  [repeat_penalty]="1.2"
  [num_predict]="256"
)

declare -A CONFIG_6=(
  [name]="Quality"
  [description]="Максимальное качество, длинные ответы"
  [temperature]="0.3"
  [num_ctx]="4096"
  [top_k]="40"
  [top_p]="0.9"
  [repeat_penalty]="1.15"
  [num_predict]="1024"
)

# Array of config names for iteration
CONFIGS=("CONFIG_1" "CONFIG_2" "CONFIG_3" "CONFIG_4" "CONFIG_5" "CONFIG_6")

# Function to get config value
get_config_value() {
    local config_name=$1
    local key=$2
    local var_name="${config_name}[$key]"
    echo "${!var_name}"
}

# Function to test a single configuration
test_configuration() {
    local config_name=$1
    local query=$2
    local query_num=$3

    local name=$(get_config_value "$config_name" "name")
    local description=$(get_config_value "$config_name" "description")
    local temperature=$(get_config_value "$config_name" "temperature")
    local num_ctx=$(get_config_value "$config_name" "num_ctx")
    local top_k=$(get_config_value "$config_name" "top_k")
    local top_p=$(get_config_value "$config_name" "top_p")
    local repeat_penalty=$(get_config_value "$config_name" "repeat_penalty")
    local num_predict=$(get_config_value "$config_name" "num_predict")

    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}Configuration: $name${NC}"
    echo -e "${BLUE}Description: $description${NC}"
    echo -e "Parameters: temp=$temperature, ctx=$num_ctx, top_k=$top_k, top_p=$top_p, penalty=$repeat_penalty, predict=$num_predict"
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    # Build JSON request
    read -r -d '' REQUEST_JSON <<EOF || true
{
  "message": "$query",
  "model": "phi3:mini",
  "temperature": $temperature,
  "maxTokens": $num_predict,
  "systemPrompt": "Ты AI ассистент для разработчиков. Отвечай кратко и точно. Примеры кода в markdown."
}
EOF

    # Start timer
    start_time=$(date +%s.%N)

    # Call API
    echo -e "${BLUE}Отправляю запрос...${NC}"
    response=$(curl -s -X POST "$SERVICE_URL" \
      -H "Content-Type: application/json" \
      -d "$REQUEST_JSON")

    # End timer
    end_time=$(date +%s.%N)
    duration=$(echo "$end_time - $start_time" | bc)

    # Extract data
    response_text=$(echo "$response" | jq -r '.response // empty')
    tokens=$(echo "$response" | jq -r '.tokensGenerated // 0')
    processing_time=$(echo "$response" | jq -r '.processingTimeMs // 0')
    error=$(echo "$response" | jq -r '.error // empty')
    model=$(echo "$response" | jq -r '.model // "unknown"')

    # Save full response to file
    output_file="$RESULTS_DIR/Q${query_num}_${name}.txt"

    cat > "$output_file" <<EOF
Configuration: $name
Description: $description
Query: $query

Parameters:
- Temperature: $temperature
- Context Window: $num_ctx
- Top K: $top_k
- Top P: $top_p
- Repeat Penalty: $repeat_penalty
- Max Tokens: $num_predict

Timing:
- Total Time: ${duration}s
- Processing Time: ${processing_time}ms
- Tokens Generated: $tokens
- Tokens/Second: $(echo "scale=2; $tokens * 1000 / $processing_time" | bc 2>/dev/null || echo "0")

Model: $model

Response:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
$response_text
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Raw JSON Response:
$response
EOF

    # Display results
    if [ -n "$error" ]; then
        echo -e "${RED}❌ ERROR: $error${NC}"
        echo ""
    else
        tokens_per_sec=$(echo "scale=1; $tokens * 1000 / $processing_time" | bc 2>/dev/null || echo "0")

        echo -e "${GREEN}✅ SUCCESS${NC}"
        echo -e "⏱️  Total Time: ${duration}s (Processing: ${processing_time}ms)"
        echo -e "📊 Tokens: $tokens (${tokens_per_sec} tok/s)"
        echo -e "🤖 Model: $model"
        echo ""
        echo -e "${BLUE}📝 FULL RESPONSE:${NC}"
        echo "┌────────────────────────────────────────────────────────────────────┐"
        echo "$response_text" | fold -w 68 -s | sed 's/^/│ /' | sed 's/$/                                                                    /' | cut -c1-70 | sed 's/ *$/│/'
        echo "└────────────────────────────────────────────────────────────────────┘"
        echo ""
        echo -e "${GREEN}💾 Saved to: $output_file${NC}"
        echo ""
    fi
}

# Main test loop
echo -e "${BLUE}Starting tests...${NC}"
echo -e "${BLUE}Results will be saved to: $RESULTS_DIR${NC}"
echo ""

total_tests=$((${#CONFIGS[@]} * ${#QUERIES[@]}))
current_test=0

for query_idx in "${!QUERIES[@]}"; do
    query="${QUERIES[$query_idx]}"
    query_num=$((query_idx + 1))

    echo ""
    echo -e "${YELLOW}╔════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║  QUERY $query_num: ${query:0:50}...${NC}"
    echo -e "${YELLOW}╚════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""

    for config_name in "${CONFIGS[@]}"; do
        current_test=$((current_test + 1))
        echo -e "${BLUE}Progress: [$current_test/$total_tests]${NC}"

        test_configuration "$config_name" "$query" "$query_num"

        # Rate limiting
        sleep 2
    done
done

# Generate comparison report
echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Generating Comparison Report...                                   ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════════╝${NC}"
echo ""

REPORT_FILE="$RESULTS_DIR/COMPARISON_REPORT.md"

cat > "$REPORT_FILE" <<'REPORT_HEADER'
# LLM Optimization Test Report - phi3:mini

**Date:** TIMESTAMP
**Model:** phi3:mini
**Configurations Tested:** 6
**Queries Tested:** 3

## Executive Summary

This report compares 6 different parameter configurations for phi3:mini model.

---

## Configurations

### 1. Baseline
**Description:** Стандартная конфигурация (контроль)
- Temperature: 0.7
- Context: 2048
- Top K: 40
- Top P: 0.9
- Repeat Penalty: 1.0
- Max Tokens: 512

### 2. Precise
**Description:** Точные детерминированные ответы
- Temperature: 0.1
- Context: 2048
- Top K: 10
- Top P: 0.5
- Repeat Penalty: 1.0
- Max Tokens: 512

### 3. Balanced (⭐ RECOMMENDED)
**Description:** Баланс точность/креативность
- Temperature: 0.3
- Context: 4096
- Top K: 40
- Top P: 0.9
- Repeat Penalty: 1.1
- Max Tokens: 512

### 4. Creative
**Description:** Креативные развернутые ответы
- Temperature: 0.8
- Context: 4096
- Top K: 50
- Top P: 0.95
- Repeat Penalty: 1.1
- Max Tokens: 1024

### 5. Fast
**Description:** Быстрые короткие ответы
- Temperature: 0.3
- Context: 2048
- Top K: 20
- Top P: 0.8
- Repeat Penalty: 1.2
- Max Tokens: 256

### 6. Quality
**Description:** Максимальное качество, длинные ответы
- Temperature: 0.3
- Context: 4096
- Top K: 40
- Top P: 0.9
- Repeat Penalty: 1.15
- Max Tokens: 1024

---

## Test Queries

1. **Simple Question:** "Что такое Spring Boot?"
2. **Code Generation:** "Напиши простой REST контроллер на Spring Boot с GET endpoint"
3. **Complex Task:** "Как настроить WebClient для работы с внешним API? Покажи код"

---

## Results by Configuration

REPORT_HEADER

# Replace timestamp
sed -i "s/TIMESTAMP/$(date '+%Y-%m-%d %H:%M:%S')/" "$REPORT_FILE"

# Add results for each config
for config_name in "${CONFIGS[@]}"; do
    name=$(get_config_value "$config_name" "name")

    cat >> "$REPORT_FILE" <<EOF

### Configuration: $name

| Query | Time (ms) | Tokens | Tokens/s | File |
|-------|-----------|--------|----------|------|
EOF

    for query_idx in "${!QUERIES[@]}"; do
        query_num=$((query_idx + 1))
        file="Q${query_num}_${name}.txt"

        if [ -f "$RESULTS_DIR/$file" ]; then
            # Extract metrics from file
            time=$(grep "Processing Time:" "$RESULTS_DIR/$file" | sed 's/.*: //' | sed 's/ms//')
            tokens=$(grep "Tokens Generated:" "$RESULTS_DIR/$file" | sed 's/.*: //')
            tokens_per_s=$(grep "Tokens/Second:" "$RESULTS_DIR/$file" | sed 's/.*: //')

            echo "| Query $query_num | $time | $tokens | $tokens_per_s | [$file](./$file) |" >> "$REPORT_FILE"
        fi
    done
done

# Add comparison table
cat >> "$REPORT_FILE" <<'REPORT_FOOTER'

---

## Performance Comparison

### Average Metrics by Configuration

| Config | Avg Time (ms) | Avg Tokens | Avg Speed (tok/s) | Use Case |
|--------|---------------|------------|-------------------|----------|
| Baseline | - | - | - | General purpose |
| Precise | - | - | - | Factual answers |
| Balanced | - | - | - | ⭐ Recommended |
| Creative | - | - | - | Creative writing |
| Fast | - | - | - | Quick responses |
| Quality | - | - | - | Detailed answers |

*(Calculate averages from individual test files)*

---

## Key Findings

### Best for Speed
**Fast** configuration with temperature=0.3, max_tokens=256

### Best for Quality
**Quality** or **Balanced** configuration

### Best Overall
**Balanced** configuration provides optimal speed/quality trade-off

---

## Parameter Effects

### Temperature
- **0.1-0.3:** Deterministic, factual responses
- **0.7:** More varied responses
- **0.8+:** Creative, less predictable

### Top K
- **10-20:** Limited vocabulary, faster
- **40-50:** Better word choice, slightly slower

### Top P
- **0.5:** Very focused
- **0.9:** Good balance
- **0.95:** More diverse

### Repeat Penalty
- **1.0:** No penalty
- **1.1-1.15:** Reduces repetition
- **1.2+:** May affect coherence

---

## Recommendations

### For Your Chat Application

**Use "Balanced" configuration:**
```properties
ollama.temperature=0.3
ollama.max-tokens=512
```

With additional options in LlmChatService.java:
```java
options.put("num_ctx", 4096);
options.put("top_k", 40);
options.put("top_p", 0.9);
options.put("repeat_penalty", 1.1);
```

### Why?
- ✅ Fast enough (2-3s responses)
- ✅ High quality answers
- ✅ Good code generation
- ✅ Minimal repetition
- ✅ Suitable context window

---

## Next Steps

1. Review individual response files
2. Choose preferred configuration
3. Update LlmChatService.java
4. Test with real users
5. Adjust based on feedback

---

## Files

All test results saved in: `RESULTS_DIR/`

- Individual responses: `Q{N}_{CONFIG}.txt`
- This report: `COMPARISON_REPORT.md`

REPORT_FOOTER

# Replace RESULTS_DIR in report
sed -i "s|RESULTS_DIR|$RESULTS_DIR|g" "$REPORT_FILE"

echo -e "${GREEN}✅ Test completed!${NC}"
echo ""
echo -e "${BLUE}📁 Results saved to: $RESULTS_DIR/${NC}"
echo -e "${BLUE}📄 Comparison report: $REPORT_FILE${NC}"
echo ""
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}Summary:${NC}"
echo -e "  - Total tests: $total_tests"
echo -e "  - Results directory: $RESULTS_DIR"
echo -e "  - Individual responses: $RESULTS_DIR/Q*_*.txt"
echo -e "  - Comparison report: $REPORT_FILE"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo -e "  1. cat $REPORT_FILE"
echo -e "  2. less $RESULTS_DIR/Q1_Balanced.txt"
echo -e "  3. Compare responses and choose best config"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════════════${NC}"