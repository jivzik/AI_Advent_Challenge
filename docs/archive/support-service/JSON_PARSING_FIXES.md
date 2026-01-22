# JSON Parsing Fixes - Zusammenfassung

## ✅ Behobene Probleme

### 1. LocalDateTime Serialisierung
**Problem:** `InvalidDefinitionException: Java 8 date/time type LocalDateTime not supported`

**Lösung:**
- Dependency `jackson-datatype-jsr310` zu `pom.xml` hinzugefügt
- `ObjectMapper` in `WebConfig.java` erweitert um `JavaTimeModule`
- ISO-8601 String-Format statt Timestamps konfiguriert

**Code:**
```java
@Bean
public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
}
```

---

### 2. Markdown Code-Block Parsing
**Problem:** LLM-Antworten mit ` ```json ... ``` ` wurden als Plain-Text statt JSON erkannt

**Lösung:**
- `JsonResponseParser.canParse()` erweitert um Erkennung von Markdown Code-Blöcken
- `@Order` Annotationen hinzugefügt für korrekte Parser-Priorität
  - `JsonResponseParser`: `@Order(1)` (höchste Priorität)
  - `TextResponseParser`: `@Order(10)` (niedrigste Priorität, Fallback)

**Code:**
```java
@Override
public boolean canParse(String response) {
    String trimmed = response.trim();
    
    // Prüfe ob es ein Markdown-Code-Block mit JSON ist
    if (trimmed.startsWith("```json") || trimmed.startsWith("```JSON")) {
        return true;
    }
    
    // Prüfe ob es ein generischer Code-Block ist, der JSON enthält
    if (trimmed.startsWith("```") && trimmed.contains("{")) {
        return true;
    }
    
    // ... rest der Logik
}
```

---

### 3. Unescaped Kontrollzeichen in JSON
**Problem:** `Illegal unquoted character (CTRL-CHAR, code 10): has to be escaped`
- LLMs generieren manchmal JSON mit unescaped Newlines (`\n`), Tabs (`\t`) etc. in Strings
- JSON-Parser lehnt dies als ungültiges JSON ab

**Lösung:**
- Neue Methode `fixUnescapedControlChars()` implementiert
- Scannt JSON-String character-by-character
- Escaped automatisch Kontrollzeichen in JSON-String-Werten:
  - `\n` → `\\n`
  - `\r` → `\\r`
  - `\t` → `\\t`
  - Andere Kontrollzeichen → Unicode escape (`\uXXXX`)

**Code:**
```java
private String fixUnescapedControlChars(String json) {
    StringBuilder result = new StringBuilder();
    boolean inString = false;
    boolean escaped = false;
    
    for (int i = 0; i < json.length(); i++) {
        char c = json.charAt(i);
        
        // Track ob wir in einem String sind
        // Wenn ja, escape Kontrollzeichen
        if (inString && c == '\n') {
            result.append("\\n");
        } else if (inString && Character.isISOControl(c)) {
            result.append(String.format("\\u%04x", (int) c));
        } else {
            result.append(c);
        }
    }
    
    return result.toString();
}
```

---

### 4. Tool Execution Fehlerbehandlung
**Problem:** Wenn `step: "tool"` aber keine gültigen `tool_calls`, wurde der Loop nicht korrekt beendet

**Lösung:**
- Erweiterte Fehlerbehandlung in `ToolExecutionOrchestrator`
- Verschiedene Szenarien behandeln:
  1. `step: "tool"` mit `tool_calls` → Tools ausführen
  2. `step: "tool"` ohne `tool_calls` → Als final behandeln
  3. `step: null` oder leer → Als final behandeln
  4. Unbekannter step → Als final behandeln

**Code:**
```java
if (STEP_TOOL.equals(parsed.getStep())) {
    if (hasToolCalls(parsed)) {
        executeTools(parsed, messages, sources);
    } else {
        log.warn("⚠️ Step is 'tool' but no tool_calls found, treating answer as final");
        return formatFinalAnswer(parsed.getAnswer(), sources);
    }
}
```

---

## 🧪 Testing

### Neukompilierung erforderlich
```bash
cd backend/support-service
mvn clean compile
```

### Service neu starten
```bash
mvn spring-boot:run
```

### Test-Szenarien

1. **Remote LLM (OpenRouter)**
   - Sollte wie bisher funktionieren
   - JSON wird korrekt geparst

2. **Local LLM (Ollama)**
   - Markdown Code-Blocks werden erkannt
   - Newlines in Antworten werden korrekt escaped
   - Tool-Calls funktionieren

3. **LocalDateTime Serialisierung**
   - API-Responses enthalten `timestamp` als ISO-8601 String
   - Beispiel: `"timestamp": "2026-01-20T16:42:11.038"`

---

## 📝 Geänderte Dateien

1. **pom.xml**
   - `jackson-datatype-jsr310` dependency hinzugefügt

2. **WebConfig.java**
   - `ObjectMapper` mit `JavaTimeModule` konfiguriert

3. **JsonResponseParser.java**
   - `canParse()` erweitert für Markdown Code-Blocks
   - `cleanJsonResponse()` erweitert
   - `fixUnescapedControlChars()` neu implementiert
   - `@Order(1)` Annotation hinzugefügt

4. **TextResponseParser.java**
   - `@Order(10)` Annotation hinzugefügt

5. **ToolExecutionOrchestrator.java**
   - Erweiterte Fehlerbehandlung für verschiedene Step-Szenarien
   - Besseres Logging

---

## 🎯 Erwartetes Verhalten

### Vorher (Fehler)
```
❌ Illegal unquoted character (CTRL-CHAR, code 10)
❌ Cannot parse JSON response
❌ Treating as plain text instead of JSON
```

### Nachher (Funktioniert)
```
✅ Detected Markdown code block
✅ Fixed unescaped control characters
✅ Successfully parsed JSON response
✅ Executing tools / Returning final answer
```

---

## 🔍 Debugging

Falls weiterhin Probleme auftreten, prüfen Sie:

1. **Logs anschauen:**
   ```
   🔵 LLM raw response: ...
   📝 Parsing response as ...
   ✅ Successfully parsed JSON response
   ```

2. **JSON-Format validieren:**
   - Ist es ein Markdown Code-Block?
   - Enthält es unescaped Newlines?
   - Ist die Struktur korrekt?

3. **Parser-Reihenfolge:**
   - JsonResponseParser sollte zuerst geprüft werden (@Order(1))
   - TextResponseParser als Fallback (@Order(10))

---

## ✅ Status

- [x] LocalDateTime Serialisierung behoben
- [x] Markdown Code-Block Parsing implementiert
- [x] Unescaped Kontrollzeichen Handling implementiert
- [x] Parser-Priorität konfiguriert
- [x] Tool Execution Fehlerbehandlung verbessert
- [x] Kompilierung erfolgreich
- [x] Bereit zum Testen

Der Service sollte jetzt sowohl mit Remote (OpenRouter) als auch Local (Ollama) LLMs korrekt funktionieren! 🎉

