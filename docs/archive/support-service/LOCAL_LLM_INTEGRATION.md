# Local LLM Integration (Ollama)

## Übersicht

Der Support-Service unterstützt jetzt sowohl **lokale LLM** (Ollama) als auch **remote LLM** (OpenRouter). Der Benutzer kann im Frontend zwischen den beiden Optionen wechseln.

## Architektur

### Backend-Komponenten

1. **OllamaProperties** (`config/OllamaProperties.java`)
   - Konfigurationsklasse für Ollama-Einstellungen
   - Properties mit Prefix `llm.ollama`

2. **OllamaWebClientConfig** (`config/OllamaWebClientConfig.java`)
   - WebClient-Konfiguration für Ollama API
   - Connection Pooling, Timeouts, Error Handling

3. **OllamaApiClient** (`service/client/OllamaApiClient.java`)
   - Client für Ollama API-Kommunikation
   - Analog zu OpenRouterApiClient

4. **ToolExecutionOrchestrator** (erweitert)
   - Unterstützt Provider-Parameter (`local` oder `remote`)
   - Wählt automatisch den richtigen Client

5. **SupportChatService** (erweitert)
   - Nimmt `llmProvider` aus dem Request
   - Übergibt Provider an ToolExecutionOrchestrator

### Frontend-Komponenten

1. **SupportChat.vue** (erweitert)
   - Toggle-Button für LLM Provider
   - State: `llmProvider` ('local' | 'remote')
   - Sendet Provider-Präferenz an Backend

2. **supportChatService.ts** (erweitert)
   - Interface `SendMessageRequest` erweitert um `llmProvider`

## Konfiguration

### application.properties

```properties
# Ollama Local LLM Configuration
llm.ollama.base-url=http://localhost:11434
llm.ollama.model=gemma2:2b
llm.ollama.temperature=0.7
llm.ollama.max-tokens=1000
llm.ollama.timeout-seconds=120
```

### Ollama starten

```bash
# Ollama installieren (falls noch nicht geschehen)
# https://ollama.ai/download

# Modell herunterladen
ollama pull gemma2:2b

# Ollama läuft automatisch im Hintergrund
# API ist unter http://localhost:11434 verfügbar
```

## Verwendung

### Frontend

1. Öffne Support Chat
2. Klicke auf den Provider-Toggle-Button im Header
3. Wechsle zwischen:
   - **☁️ Remote (OpenRouter)** - Verwendet Claude 3.5 Sonnet
   - **🤖 Local (Ollama)** - Verwendet gemma2:2b

### API Request

```json
{
  "userEmail": "user@example.com",
  "message": "Wie funktioniert die Authentifizierung?",
  "llmProvider": "local"
}
```

## Vorteile

### Remote LLM (OpenRouter)
- ✅ Leistungsstärkere Modelle (Claude 3.5 Sonnet)
- ✅ Bessere Tool-Verwendung und Reasoning
- ✅ Keine lokale Hardware erforderlich
- ❌ Erfordert API-Key und Internet
- ❌ Kosten pro Request

### Local LLM (Ollama)
- ✅ Keine Kosten
- ✅ Datenschutz (Daten bleiben lokal)
- ✅ Offline-Betrieb möglich
- ✅ Schnelle Antworten (kein Netzwerk-Overhead)
- ❌ Benötigt lokale GPU/CPU-Ressourcen
- ❌ Kleineres Modell (gemma2:2b)

## Testing

### Backend kompilieren

```bash
cd backend/support-service
mvn clean compile
```

### Service starten

```bash
cd backend/support-service
mvn spring-boot:run
```

### Frontend starten

```bash
cd frontend
npm run dev
```

### Test im Browser

1. Öffne http://localhost:5173
2. Navigiere zu Support Chat
3. Teste beide Provider-Modi:
   - Stelle eine Frage mit Remote Provider
   - Wechsle zu Local Provider
   - Stelle dieselbe Frage erneut
   - Vergleiche die Antworten

## Troubleshooting

### Ollama Connection Error

**Problem:** `Failed to call Ollama API`

**Lösung:**
```bash
# Prüfe ob Ollama läuft
curl http://localhost:11434/api/version

# Starte Ollama neu
ollama serve
```

### Modell nicht gefunden

**Problem:** `model 'gemma2:2b' not found`

**Lösung:**
```bash
# Modell herunterladen
ollama pull gemma2:2b

# Verfügbare Modelle anzeigen
ollama list
```

### Timeout Errors

**Problem:** Anfragen laufen in Timeout

**Lösung:**
- Erhöhe `llm.ollama.timeout-seconds` in application.properties
- Verwende ein kleineres Modell
- Reduziere `llm.ollama.max-tokens`

## Weitere Modelle

### Andere Ollama Modelle verwenden

```properties
# Llama 2 (7B)
llm.ollama.model=llama2

# Mistral (7B)
llm.ollama.model=mistral

# CodeLlama (für Code-Fragen)
llm.ollama.model=codellama

# Gemma 2 (9B - größeres Modell)
llm.ollama.model=gemma2:9b
```

Modell ändern und Service neu starten.

## Performance-Tipps

1. **Kleineres Modell für schnellere Antworten**: `gemma2:2b`
2. **Größeres Modell für bessere Qualität**: `gemma2:9b` oder `llama2`
3. **GPU verwenden** für deutlich schnellere Inference
4. **Temperature reduzieren** (0.3-0.5) für deterministischere Antworten
5. **max-tokens limitieren** für schnellere Responses

## Nächste Schritte

- [ ] Provider-Präferenz im LocalStorage speichern
- [ ] Performance-Metriken anzeigen (Response Time, Tokens/s)
- [ ] Modell-Auswahl im Frontend ermöglichen
- [ ] Streaming-Support für Ollama
- [ ] Fehlerbehandlung verbessern

