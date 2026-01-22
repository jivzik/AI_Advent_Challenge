#!/bin/bash

# Test-Script für RAG Upload mit Metadaten
# Zeigt, wie man Dokumente mit benutzerdefinierten Metadaten hochlädt

BASE_URL="http://localhost:8080"

echo "🚀 Testing RAG Document Upload with Metadata"
echo "=========================================="

# Test 1: Upload ohne Metadaten (wie bisher)
echo ""
echo "📄 Test 1: Upload ohne Metadaten"
curl -X POST "$BASE_URL/api/documents/upload" \
  -F "file=@test-document.txt"

# Test 2: Upload mit einfachen Metadaten
echo ""
echo "📄 Test 2: Upload mit einfachen Metadaten"
curl -X POST "$BASE_URL/api/documents/upload" \
  -F "file=@test-document.txt" \
  -F 'metadata={"category":"technical","language":"de","tags":["test","documentation"]}'

# Test 3: Upload mit erweiterten Metadaten
echo ""
echo "📄 Test 3: Upload mit erweiterten Metadaten"
curl -X POST "$BASE_URL/api/documents/upload" \
  -F "file=@test-document.txt" \
  -F 'metadata={"category":"user-manual","department":"engineering","version":"1.0.0","author":"Max Mustermann","project":"AI_Advent_Challenge","tags":["rag","vector-database","pgvector"],"priority":"high","confidential":false}'

# Test 4: Alle Dokumente abrufen (um Metadaten zu sehen)
echo ""
echo "📋 Test 4: Alle Dokumente mit Metadaten abrufen"
curl -X GET "$BASE_URL/api/documents" | jq '.'

echo ""
echo "✅ Tests abgeschlossen!"

