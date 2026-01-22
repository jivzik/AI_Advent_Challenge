#!/bin/bash

#######################################
# Тестовый скрипт для index-project-docs.sh
# Проверяет основную функциональность без запуска RAG сервиса
#######################################

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INDEX_SCRIPT="${SCRIPT_DIR}/index-project-docs.sh"

print_test() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

print_pass() {
    echo -e "${GREEN}[PASS]${NC} ✓ $1"
}

print_fail() {
    echo -e "${RED}[FAIL]${NC} ✗ $1"
    exit 1
}

echo "========================================="
echo "Testing index-project-docs.sh"
echo "========================================="

# Test 1: Script exists and is executable
print_test "Проверка существования скрипта..."
if [ ! -f "$INDEX_SCRIPT" ]; then
    print_fail "Скрипт не найден: $INDEX_SCRIPT"
fi
if [ ! -x "$INDEX_SCRIPT" ]; then
    print_fail "Скрипт не исполняемый: $INDEX_SCRIPT"
fi
print_pass "Скрипт найден и исполняемый"

# Test 2: Help option works
print_test "Проверка --help..."
if ! "$INDEX_SCRIPT" --help > /dev/null 2>&1; then
    print_fail "Опция --help не работает"
fi
print_pass "Опция --help работает"

# Test 3: Dry-run mode works
print_test "Проверка --dry-run..."
OUTPUT=$("$INDEX_SCRIPT" --dry-run 2>&1)
if ! echo "$OUTPUT" | grep -q "DRY-RUN"; then
    print_fail "Режим --dry-run не работает"
fi
if ! echo "$OUTPUT" | grep -q "Найдено файлов:"; then
    print_fail "Скрипт не находит файлы"
fi
print_pass "Режим --dry-run работает"

# Test 4: Finds expected documentation files
print_test "Проверка поиска файлов документации..."
OUTPUT=$("$INDEX_SCRIPT" --dry-run 2>&1)

# Проверяем наличие основных файлов
EXPECTED_FILES=(
    "README.md"
    "FEATURES_INDEX.md"
    "OPENROUTER_QUICKSTART.md"
    "RAG_MCP_INTEGRATION.md"
)

for file in "${EXPECTED_FILES[@]}"; do
    if ! echo "$OUTPUT" | grep -q "$file"; then
        print_fail "Файл не найден в выводе: $file"
    fi
done
print_pass "Все ожидаемые файлы найдены"

# Test 5: Categories are assigned correctly
print_test "Проверка назначения категорий..."
if ! echo "$OUTPUT" | grep -q "категория: project-root"; then
    print_fail "Категория project-root не найдена"
fi
if ! echo "$OUTPUT" | grep -q "категория: quickstarts"; then
    print_fail "Категория quickstarts не найдена"
fi
if ! echo "$OUTPUT" | grep -q "категория: architecture"; then
    print_fail "Категория architecture не найдена"
fi
print_pass "Категории назначены корректно"

# Test 6: Statistics are shown
print_test "Проверка вывода статистики..."
if ! echo "$OUTPUT" | grep -q "Всего файлов найдено:"; then
    print_fail "Статистика 'Всего файлов' не найдена"
fi
if ! echo "$OUTPUT" | grep -q "Успешно загружено:"; then
    print_fail "Статистика 'Успешно загружено' не найдена"
fi
print_pass "Статистика выводится корректно"

# Test 7: File count is reasonable
print_test "Проверка количества найденных файлов..."
FILE_COUNT=$(echo "$OUTPUT" | grep -oP 'Найдено файлов: \K\d+')
if [ "$FILE_COUNT" -lt 20 ]; then
    print_fail "Найдено слишком мало файлов: $FILE_COUNT (ожидалось > 20)"
fi
if [ "$FILE_COUNT" -gt 100 ]; then
    print_fail "Найдено слишком много файлов: $FILE_COUNT (ожидалось < 100)"
fi
print_pass "Найдено разумное количество файлов: $FILE_COUNT"

# Test 8: Invalid option handling
print_test "Проверка обработки неверных опций..."
if "$INDEX_SCRIPT" --invalid-option > /dev/null 2>&1; then
    print_fail "Скрипт должен завершиться с ошибкой для неверной опции"
fi
print_pass "Неверные опции обрабатываются корректно"

# Test 9: curl dependency check
print_test "Проверка зависимости curl..."
if ! command -v curl &> /dev/null; then
    print_fail "curl не установлен (требуется для работы скрипта)"
fi
print_pass "curl установлен"

# Test 10: Script can be run from different directories
print_test "Проверка запуска из разных директорий..."
cd /tmp
OUTPUT=$("$INDEX_SCRIPT" --dry-run 2>&1)
if ! echo "$OUTPUT" | grep -q "README.md"; then
    print_fail "Скрипт не работает при запуске из другой директории"
fi
cd "$SCRIPT_DIR"
print_pass "Скрипт работает из любой директории"

echo ""
echo "========================================="
echo -e "${GREEN}Все тесты пройдены успешно!${NC}"
echo "========================================="
echo ""
echo "Для реальной индексации документации:"
echo "1. Запустите RAG сервис: cd backend/rag-mcp-server && mvn spring-boot:run"
echo "2. Запустите скрипт: ./index-project-docs.sh"

