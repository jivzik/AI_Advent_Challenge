# Temperature Control Feature

## Übersicht

Der Temperature-Parameter wurde erfolgreich zum Chat-Interface hinzugefügt. Er ermöglicht es Benutzern, den Kreativitätsgrad der KI-Antworten zu steuern.

## Was ist Temperature?

Temperature ist ein Parameter, der die Zufälligkeit und Kreativität der KI-Antworten steuert:

- **Niedrige Werte (0.0 - 0.3)**: Maximale Präzision und Vorhersagbarkeit
  - Beste Wahl für: Faktenbasierte Fragen, technische Dokumentation, präzise Berechnungen
  - Die KI wählt immer die wahrscheinlichste Antwort
  - Konsistente, wiederholbare Ergebnisse

- **Mittlere Werte (0.4 - 0.9)**: Balance zwischen Präzision und Kreativität
  - Beste Wahl für: Allgemeine Konversation, Brainstorming, Problemlösung
  - Standard-Wert: **0.7**
  - Gute Balance für die meisten Anwendungsfälle

- **Hohe Werte (1.0 - 2.0)**: Maximale Kreativität und Variation
  - Beste Wahl für: Kreatives Schreiben, Ideen-Generierung, künstlerische Aufgaben
  - Unvorhersehbarere, aber kreativere Antworten
  - **Achtung**: Bei sehr hohen Werten können die Antworten inkohärent werden

## UI-Implementierung

### Slider-Steuerung

- **Typ**: Horizontaler Slider (Range Input)
- **Bereich**: 0.0 - 2.0
- **Schrittweite**: 0.1
- **Standardwert**: 0.7
- **Position**: Unterhalb des System Prompt-Bereichs

### Visuelle Elemente

1. **Header**:
   - Icon: 🌡️
   - Label: "Temperature"
   - Aktueller Wert (z.B. "0.7") wird rechts angezeigt

2. **Beschreibung**:
   - Dynamische Beschreibung basierend auf dem gewählten Wert
   - 0.0-0.3: "Строгая точность, минимум фантазии"
   - 0.4-0.9: "Баланс точности и креативности"
   - 1.0-2.0: "Максимальная креативность, возможен бред"

3. **Slider**:
   - Farbverlauf von Blau (präzise) zu Rot (kreativ)
   - Weißer Thumb mit blauem Rahmen
   - Hover- und Active-States für bessere UX

4. **Bereichslabels**:
   - Links: "0 — Точность"
   - Mitte: "1 — Баланс"
   - Rechts: "2 — Креативность"

## Backend-Implementierung

### Geänderte Dateien

#### 1. `ChatRequest.java`
```java
private Double temperature = 0.7; // Default value
```
- Neues Feld mit Standardwert 0.7
- Getter und Setter hinzugefügt

#### 2. `PerplexityRequest.java`
```java
private Double temperature;
```
- Temperature-Feld zum Request-DTO hinzugefügt
- Builder-Pattern unterstützt temperature
- Neuer Konstruktor mit temperature-Parameter

#### 3. `AgentService.java`
```java
String rawReply = getLlmResponse(history, request.getTemperature());
```
- Temperature wird an den Perplexity Client weitergegeben
- Methoden-Signatur aktualisiert

#### 4. `PerplexityToolClient.java`
```java
public String requestCompletion(List<Message> messages, Double temperature)
```
- Neue überladene Methode mit temperature-Parameter
- Temperature wird im API-Request mitgesendet
- Rückwärtskompatibilität durch Default-Wert (0.7)

## Frontend-Implementierung

### Geänderte Dateien

#### 1. `types.ts` (Types)
```typescript
temperature?: number;
```
- Temperature-Feld zu `ChatRequest` Interface hinzugefügt

#### 2. `chatService.ts`
```typescript
temperature: options.temperature
```
- Temperature wird im API-Request mitgesendet
- Interface `SendMessageOptions` erweitert

#### 3. `ChatInterface.vue`
- Neue reactive Variable: `const temperature = ref(0.7)`
- `getTemperatureDescription()` Funktion für dynamische Beschreibungen
- Temperature-Slider UI-Komponente
- Temperature wird beim Senden der Nachricht übergeben

#### 4. `_components.scss`
- Komplettes Styling für `.temperature-section`
- Responsive Slider mit Farbverlauf
- Ansprechende Hover- und Active-States
- Mobile-freundliches Design

## API-Request-Beispiel

```json
{
  "message": "Schreibe ein Gedicht über KI",
  "userId": "user-123",
  "conversationId": "conv-456",
  "systemPrompt": "Du bist ein kreativer Assistent",
  "temperature": 1.5,
  "jsonMode": false
}
```

## Verwendungsbeispiele

### Beispiel 1: Präzise Fakten (Temperature: 0.1)
**Frage**: "Was ist die Hauptstadt von Deutschland?"
**Erwartung**: Immer "Berlin" - keine Variation

### Beispiel 2: Normale Konversation (Temperature: 0.7)
**Frage**: "Wie kann ich meine Produktivität steigern?"
**Erwartung**: Hilfreiche, aber leicht variierende Tipps

### Beispiel 3: Kreatives Schreiben (Temperature: 1.5)
**Frage**: "Schreibe eine Science-Fiction-Geschichte"
**Erwartung**: Sehr kreative, unvorhersehbare Geschichten

## Technische Details

### Perplexity API
- Der Temperature-Parameter wird direkt an die Perplexity API weitergeleitet
- Perplexity unterstützt Werte von 0.0 bis 2.0
- Der Parameter beeinflusst die Sampling-Strategie der KI

### Persistenz
- Der Temperature-Wert ist **nicht** Teil der Konversationshistorie
- Er kann für jede Nachricht individuell gesetzt werden
- Bei Seiten-Reload wird der Standardwert (0.7) wiederhergestellt

## Zukünftige Erweiterungen

Mögliche Verbesserungen:
1. **Presets**: Vordefinierte Temperature-Werte (z.B. "Präzise", "Ausgewogen", "Kreativ")
2. **Persistenz**: Temperature-Einstellung im localStorage speichern
3. **Pro-Nachricht**: Verschiedene Temperature-Werte für verschiedene Nachrichten in der Historie
4. **Empfehlungen**: Intelligente Vorschläge basierend auf der Frage-Art
5. **Tooltips**: Erweiterte Erklärungen bei Hover über den Slider

## Testing

### Frontend-Build
```bash
cd frontend
npm run build
```
✅ Erfolgreich kompiliert

### Backend-Build
```bash
cd backend/perplexity-service
mvn clean compile -DskipTests
```
✅ Erfolgreich kompiliert

## Zusammenfassung

Das Temperature-Feature wurde vollständig implementiert und getestet:
- ✅ Backend: Temperature-Parameter in allen relevanten Klassen
- ✅ Frontend: Benutzerfreundlicher Slider mit visuellen Hinweisen
- ✅ API: Temperature wird korrekt an Perplexity weitergeleitet
- ✅ Styling: Ansprechendes, responsives Design
- ✅ Dokumentation: Vollständige Erklärung der Funktionalität

Der Benutzer kann nun die Kreativität der KI-Antworten in Echtzeit steuern!

