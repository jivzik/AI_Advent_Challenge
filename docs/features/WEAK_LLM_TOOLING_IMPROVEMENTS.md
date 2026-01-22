# Zusammenfassung: Verbesserungen für schwache LLMs bei Tool-Verwendung

## Problem
Schwächere LLMs (wie lokale Modelle) machen häufig Fehler bei der Tool-Verwendung:
1. **Unescaped Quotes in JSON-Strings** - Besonders bei russischem/Cyrillic Text
2. **Fehlende/falsche JSON-Struktur** - Markdown-Blöcke, extra Text
3. **Fehlendes Verständnis für Tool-Format** - Verwechslung von step, tool_calls, answer

## Implementierte Lösungen

### 1. Verbesserte Prompts (`system-tools.md`)
- **Schritt-für-Schritt-Anleitung** statt abstrakter Beschreibung
- **Konkrete Beispiele** für Tool-Calls und Final-Answers
- **"THINK STEP-BY-STEP" Sektion** am Ende des Prompts
- **Visuelle Hervorhebung** von kritischen Regeln

### 2. Verbesserter Korrektur-Prompt (`json-correction.md`)
- **Zeigt WAS falsch war** statt nur "ungültig"
- **Konkrete Beispiele** für richtiges Format
- **Copy-Paste-fertige Templates**

### 3. Tool-Ergebnisse-Prompt (`tool-results.md`)  
- **Klare Entscheidungshilfe**: Option A vs Option B
- **Beispiel-Workflows** für typische Szenarien
- **Klarstellung**: Meist nach Tool-Ausführung → "step":"final"

### 4. Kontextuelle Hinweise im Orchestrator
**Neue Methoden in `ToolExecutionOrchestrator.java`:**
- `addGuidanceForWeakLLMs()` - Fügt initiale Anleitung für lokale LLMs hinzu
- `buildContextualHint()` - Analysiert User-Anfrage und gibt spezifische Hinweise:
  - GitHub-Anfragen → Hinweis auf git:create_github_issue
  - Such-Anfragen → Hinweis auf rag:search_documents  
  - Task-Anfragen → Hinweis auf google:tasks_list
  - Allgemeine Fragen → Hinweis auf direkte Antwort ohne Tools
- `addExplicitFormatReminder()` - Fügt Format-Erinnerung hinzu wenn Parsing fehlschlägt
- `addToolResultsGuidance()` - Erinnert nach Tool-Ausführung an finale Antwort

### 5. Robuste JSON-Reparatur (`JsonResponseParser.java`)
**Neue Reparatur-Strategien:**
- `fixUnescapedQuotesInStrings()` - Repariert unescaped Quotes in String-Werten
  - Konservativer Ansatz: Nur offensichtliche Fälle
  - Unterscheidet zwischen String-Ende und Quote mittendrin
  - Berücksichtigt JSON-Struktur (nesting, afterColon)
- `containsCyrillic()` - Erkennt Cyrillic/Unicode-Text für besseres Logging
- Erweiterte Fehler-Logs mit Kontext-Informationen

### 6. Retry-Logik mit Guided Prompts
**In `ResponseParsingService.java`:**
- Bei Parsing-Fehler: Automatischer Retry mit Korrektur-Prompt
- Bei erneutem Fehler (nur 1. Iteration): Guided Retry mit expliziter Anleitung

## Wie es hilft

### Für schwache LLMs:
1. **Klarere Anweisungen** → Weniger Rätselraten
2. **Konkrete Beispiele** → Lernen durch Imitation  
3. **Schritt-für-Schritt** → Strukturiertes Denken
4. **Kontextuelle Hinweise** → Gezielte Hilfe basierend auf Anfrage
5. **Robuste Fehlerbehandlung** → Automatische Reparatur häufiger Fehler

### Beispiel-Workflow:
```
User: "Erstelle ein GitHub Issue für Bug XYZ"
↓
System fügt Hinweis hinzu: "For GitHub operations, use git:create_github_issue..."
↓  
LLM generiert: {"step":"tool","tool_calls":[{"name":"git:create_github_issue",...}]}
↓
Falls JSON ungültig: Automatische Reparatur von unescaped Quotes
↓
Falls immer noch ungültig: Retry mit Korrektur-Prompt
↓
Falls 1. Iteration: Guided Retry mit explizitem Format-Reminder
```

## Getestete Szenarien
✅ Cyrillic/russischer Text mit unescaped Quotes  
✅ Mehrere unescaped Quotes in einem String
✅ Bereits korrekt escaped Quotes (keine Beschädigung)
✅ JSON mit Markdown-Blöcken
✅ Extra Text vor/nach JSON

## Nächste Schritte (Optional)
- [ ] Few-Shot Learning: Erfolgreiche Beispiele im Kontext speichern
- [ ] Adaptives Prompting: Prompt-Komplexität basierend auf Modell anpassen
- [ ] Feedback-Loop: Fehlerhafte Responses für Fine-Tuning sammeln

## Wichtige Dateien
- `backend/support-service/src/main/resources/prompts/system-tools.md`
- `backend/support-service/src/main/resources/prompts/json-correction.md`
- `backend/support-service/src/main/resources/prompts/tool-results.md`
- `backend/support-service/src/main/java/de/jivz/supportservice/service/orchestrator/ToolExecutionOrchestrator.java`
- `backend/support-service/src/main/java/de/jivz/supportservice/service/parser/JsonResponseParser.java`

