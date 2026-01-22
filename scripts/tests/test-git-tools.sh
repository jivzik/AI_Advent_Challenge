#!/bin/bash

# Git Tools Provider Test Script
# Testet alle 5 Git-Tools des MCP-Servers

set -e

BASE_URL="http://localhost:8081"
API_ENDPOINT="${BASE_URL}/api/tools"

echo "🧪 Git Tools Provider Test Suite"
echo "=================================="
echo ""

# Funktion zum Ausführen eines Tools
execute_tool() {
    local tool_name=$1
    local arguments=$2

    echo "🔧 Testing: $tool_name"

    response=$(curl -s -X POST "${API_ENDPOINT}/execute" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"$tool_name\",\"arguments\":$arguments}")

    echo "$response" | jq '.'
    echo ""

    # Prüfe auf Erfolg
    success=$(echo "$response" | jq -r '.success // false')
    if [ "$success" = "true" ]; then
        echo "✅ $tool_name erfolgreich"
    else
        echo "❌ $tool_name fehlgeschlagen"
        error=$(echo "$response" | jq -r '.error // "Unknown error"')
        echo "   Error: $error"
    fi
    echo ""
}

# 1. Prüfe, ob Server läuft
echo "🔍 Prüfe Server-Status..."
if ! curl -s "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
    if ! curl -s "${BASE_URL}/api/tools" > /dev/null 2>&1; then
        echo "❌ Server läuft nicht auf ${BASE_URL}"
        echo "   Starte den Server mit: cd backend/mcp-server && mvn spring-boot:run"
        exit 1
    fi
fi
echo "✅ Server läuft"
echo ""

# 2. Liste alle verfügbaren Tools auf
echo "📋 Verfügbare Tools:"
echo "-------------------"
tools=$(curl -s "${API_ENDPOINT}")
echo "$tools" | jq -r '.[] | "- " + .name + ": " + .description'
echo ""

# Prüfe, ob Git-Tools registriert sind
git_tools_count=$(echo "$tools" | jq '[.[] | select(.name | contains("git") or contains("project"))] | length')
echo "🔍 Git-Tools gefunden: $git_tools_count"
echo ""

# 3. Test get_current_branch
echo "Test 1: get_current_branch"
echo "============================"
execute_tool "get_current_branch" "{}"

# 4. Test get_git_status
echo "Test 2: get_git_status"
echo "======================"
execute_tool "get_git_status" "{}"

# 5. Test read_project_file (README.md)
echo "Test 3: read_project_file"
echo "========================="
execute_tool "read_project_file" '{"filePath":"README.md"}'

# 6. Test list_project_files (nicht rekursiv)
echo "Test 4: list_project_files (nicht rekursiv)"
echo "============================================"
execute_tool "list_project_files" '{"directory":"."}'

# 7. Test list_project_files (rekursiv mit Filter)
echo "Test 5: list_project_files (rekursiv, nur Java)"
echo "==============================================="
execute_tool "list_project_files" '{"directory":"backend/mcp-server/src/main/java","recursive":true,"extensions":["java"]}'

# 8. Test get_git_log
echo "Test 6: get_git_log"
echo "==================="
execute_tool "get_git_log" '{"limit":5}'

# 9. Test compare_branches
echo "Test 7: compare_branches"
echo "========================"
# Get current branch first
current_branch=$(curl -s -X POST "${API_ENDPOINT}/execute" \
    -H "Content-Type: application/json" \
    -d '{"name":"get_current_branch","arguments":{}}' | jq -r '.result.branch')

echo "Aktueller Branch: $current_branch"
# Compare current branch with itself (should show no differences)
execute_tool "compare_branches" "{\"base\":\"$current_branch\",\"compare\":\"$current_branch\"}"

# 10. Sicherheitstests
echo "🔒 Sicherheitstests"
echo "==================="

echo "Test: Path Traversal (..) - sollte fehlschlagen"
execute_tool "read_project_file" '{"filePath":"../../../etc/passwd"}'

echo "Test: Absolute Path - sollte fehlschlagen"
execute_tool "read_project_file" '{"filePath":"/etc/passwd"}'

echo "Test: Nicht existierende Datei - sollte fehlschlagen"
execute_tool "read_project_file" '{"filePath":"non-existent-file.txt"}'

# 10. Zusammenfassung
echo "=================================="
echo "✅ Test-Suite abgeschlossen!"
echo "=================================="

