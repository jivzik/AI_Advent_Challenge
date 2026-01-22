#!/bin/bash

#######################################
# Скрипт автоматической индексации документации проекта в RAG систему
# Автор: AI Advent Challenge Team
# Дата: 2026-01-12
#######################################

set -e

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Конфигурация
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAG_SERVICE_URL="http://localhost:8086"
RAG_UPLOAD_ENDPOINT="${RAG_SERVICE_URL}/api/documents/upload"
RAG_DOCUMENTS_ENDPOINT="${RAG_SERVICE_URL}/api/documents"

# Счетчики
TOTAL_FILES=0
SUCCESS_COUNT=0
ERROR_COUNT=0
SKIPPED_COUNT=0

# Опции
DRY_RUN=false
FORCE_REINDEX=false

#######################################
# Функции вывода
#######################################

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[OK]${NC} ✓ $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} ✗ $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} ⚠ $1"
}

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

#######################################
# Проверка доступности RAG сервиса
#######################################

check_rag_service() {
    print_info "Проверка доступности RAG сервиса на ${RAG_SERVICE_URL}..."

    if ! curl -s --connect-timeout 5 "${RAG_SERVICE_URL}/api/documents" > /dev/null 2>&1; then
        print_error "RAG сервис недоступен на ${RAG_SERVICE_URL}"
        print_info "Запустите RAG сервис командой: cd backend/rag-mcp-server && mvn spring-boot:run"
        exit 1
    fi

    print_success "RAG сервис доступен"
}

#######################################
# Удаление существующих документов документации
#######################################

delete_existing_docs() {
    print_info "Удаление существующих документов документации..."

    # Получаем список всех документов
    local docs=$(curl -s "${RAG_DOCUMENTS_ENDPOINT}" 2>/dev/null)

    if [ -z "$docs" ]; then
        print_warning "Не удалось получить список документов"
        return
    fi

    # Извлекаем ID документов с расширением .md
    local doc_ids=$(echo "$docs" | grep -oP '"id":\s*\K\d+' 2>/dev/null || true)

    if [ -z "$doc_ids" ]; then
        print_info "Нет документов для удаления"
        return
    fi

    local deleted=0
    while IFS= read -r doc_id; do
        if curl -s -X DELETE "${RAG_DOCUMENTS_ENDPOINT}/${doc_id}" > /dev/null 2>&1; then
            ((deleted++)) || true
        fi
    done <<< "$doc_ids"

    print_success "Удалено документов: ${deleted}"
}

#######################################
# Поиск всех файлов документации
#######################################

find_documentation_files() {
    local files=()

    # Корневые файлы
    [ -f "${SCRIPT_DIR}/README.md" ] && files+=("${SCRIPT_DIR}/README.md")
    [ -f "${SCRIPT_DIR}/FEATURES_INDEX.md" ] && files+=("${SCRIPT_DIR}/FEATURES_INDEX.md")

    # Все .md файлы в docs/
    if [ -d "${SCRIPT_DIR}/docs" ]; then
        while IFS= read -r -d '' file; do
            files+=("$file")
        done < <(find "${SCRIPT_DIR}/docs" -type f -name "*.md" -print0 2>/dev/null)
    fi

    # Возвращаем массив через глобальную переменную
    FOUND_FILES=("${files[@]}")
    TOTAL_FILES=${#files[@]}
}

#######################################
# Определение категории документа
#######################################

get_document_category() {
    local filepath="$1"
    local relative_path="${filepath#${SCRIPT_DIR}/}"

    if [[ "$relative_path" == "README.md" ]] || [[ "$relative_path" == "FEATURES_INDEX.md" ]]; then
        echo "project-root"
    elif [[ "$relative_path" == *"/quickstarts/"* ]]; then
        echo "quickstarts"
    elif [[ "$relative_path" == *"/architecture/"* ]]; then
        echo "architecture"
    elif [[ "$relative_path" == *"/features/"* ]]; then
        echo "features"
    elif [[ "$relative_path" == *"/setup/"* ]]; then
        echo "setup"
    elif [[ "$relative_path" == *"/development/"* ]]; then
        echo "development"
    else
        echo "documentation"
    fi
}

#######################################
# Проверка и удаление существующего документа
#######################################

check_and_delete_existing() {
    local filename="$1"

    # Получаем список документов
    local docs=$(curl -s "${RAG_DOCUMENTS_ENDPOINT}" 2>/dev/null)

    if [ -z "$docs" ]; then
        return 0
    fi

    # Ищем документ с таким именем и извлекаем его ID
    local doc_id=$(echo "$docs" | grep -B5 "\"fileName\":\"${filename}\"" | grep -oP '"id":\s*\K\d+' | head -1 2>/dev/null || true)

    if [ -n "$doc_id" ]; then
        print_info "    Найден существующий документ (ID: ${doc_id}), удаляю..."
        curl -s -X DELETE "${RAG_DOCUMENTS_ENDPOINT}/${doc_id}" > /dev/null 2>&1
        return 0
    fi

    return 0
}

#######################################
# Загрузка файла в RAG
#######################################

upload_file_to_rag() {
    local filepath="$1"
    local filename=$(basename "$filepath")
    local filesize=$(stat -f%z "$filepath" 2>/dev/null || stat -c%s "$filepath" 2>/dev/null)
    local category=$(get_document_category "$filepath")

    # Пропускаем пустые файлы (менее 10 байт)
    if [ "$filesize" -lt 10 ]; then
        print_warning "  ${filename} - пропущен (файл пустой, ${filesize} байт)"
        ((SKIPPED_COUNT++)) || true
        return 0
    fi

    if [ $DRY_RUN = true ]; then
        print_info "  [DRY-RUN] ${filename} (${filesize} байт, категория: ${category})"
        ((SUCCESS_COUNT++)) || true
        return 0
    fi

    # Проверяем и удаляем существующий документ с таким именем
    check_and_delete_existing "$filename"

    # Выполняем загрузку
    local response=$(curl -s -w "\n%{http_code}" -X POST "${RAG_UPLOAD_ENDPOINT}" \
        -F "file=@${filepath}" \
        2>/dev/null)

    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        # Пытаемся извлечь количество чанков из ответа
        local chunks=$(echo "$body" | grep -oP '"chunkCount":\s*\K\d+' 2>/dev/null || echo "?")
        print_success "  ${filename} (${filesize} байт, ${chunks} чанков, категория: ${category})"
        ((SUCCESS_COUNT++)) || true
        return 0
    else
        local error_msg=$(echo "$body" | grep -oP '"error":\s*"\K[^"]+' 2>/dev/null || echo "Неизвестная ошибка")
        print_error "  ${filename} - ${error_msg} (HTTP ${http_code})"
        ((ERROR_COUNT++)) || true
        return 1
    fi
}

#######################################
# Обработка всех файлов
#######################################

process_all_files() {
    print_header "Обработка файлов документации"

    for file in "${FOUND_FILES[@]}"; do
        upload_file_to_rag "$file"
    done
}

#######################################
# Вывод статистики
#######################################

print_statistics() {
    print_header "Индексация завершена"

    print_info "Всего файлов найдено: ${TOTAL_FILES}"
    print_success "Успешно загружено: ${SUCCESS_COUNT}"

    if [ $SKIPPED_COUNT -gt 0 ]; then
        print_warning "Пропущено (пустые файлы): ${SKIPPED_COUNT}"
    fi

    if [ $ERROR_COUNT -gt 0 ]; then
        print_error "Ошибок: ${ERROR_COUNT}"
    else
        print_info "Ошибок: 0"
    fi

    if [ $DRY_RUN = true ]; then
        print_warning "Это был тестовый запуск (--dry-run), файлы не загружены"
    fi
}

#######################################
# Вывод справки
#######################################

show_help() {
    cat << EOF
Использование: $0 [ОПЦИИ]

Автоматическая индексация документации проекта в RAG систему.

ОПЦИИ:
    --dry-run       Показать список файлов без реальной загрузки
    --force         Удалить ВСЕ существующие документы перед индексацией
    -h, --help      Показать эту справку

ПРИМЕРЫ:
    $0                      # Загрузить все файлы (с автоматической заменой дубликатов)
    $0 --dry-run            # Показать список без загрузки
    $0 --force              # Полная переиндексация (удалить ВСЕ документы)

ПОВЕДЕНИЕ ПО УМОЛЧАНИЮ:
    - Скрипт автоматически проверяет существование файла с таким именем
    - Если файл уже индексирован, он будет удален и загружен заново
    - Это предотвращает дубликаты при повторном запуске

ТРЕБОВАНИЯ:
    - RAG сервис должен быть запущен на ${RAG_SERVICE_URL}
    - Утилита curl должна быть установлена

EOF
}

#######################################
# Основная функция
#######################################

main() {
    # Парсинг аргументов
    while [[ $# -gt 0 ]]; do
        case $1 in
            --dry-run)
                DRY_RUN=true
                shift
                ;;
            --force)
                FORCE_REINDEX=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                print_error "Неизвестная опция: $1"
                show_help
                exit 1
                ;;
        esac
    done

    print_header "Индексация документации проекта в RAG"

    # Проверка зависимостей
    if ! command -v curl &> /dev/null; then
        print_error "curl не установлен. Установите: sudo apt install curl"
        exit 1
    fi

    # Проверка RAG сервиса
    if [ $DRY_RUN = false ]; then
        check_rag_service
    else
        print_info "Режим DRY-RUN: пропускаем проверку сервиса"
    fi

    # Удаление существующих документов при --force
    if [ $FORCE_REINDEX = true ] && [ $DRY_RUN = false ]; then
        delete_existing_docs
    fi

    # Поиск файлов
    print_info "Поиск файлов документации..."
    find_documentation_files
    print_info "Найдено файлов: ${TOTAL_FILES}"

    if [ $TOTAL_FILES -eq 0 ]; then
        print_warning "Файлы документации не найдены"
        exit 0
    fi

    # Обработка файлов
    process_all_files

    # Статистика
    echo ""
    print_statistics

    # Выход с кодом ошибки если были ошибки
    if [ $ERROR_COUNT -gt 0 ]; then
        exit 1
    fi
}

# Запуск
main "$@"

