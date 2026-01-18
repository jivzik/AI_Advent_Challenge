# 📊 Metrics Display - Quickstart

## 🚀 Schneller Start

### Backend Kompilierung
```bash
cd backend/perplexity-service
mvn clean compile
```

Status: ✅ BUILD SUCCESS

### Frontend Integration
Die MetricsCard ist bereits in `ChatInterface.vue` integriert.

## 📊 Was wird angezeigt?

Nach jeder API-Antwort erscheint eine MetricsCard mit:

```
┌─────────────────────────────────────────┐
│ 📊 Response Metrics                  ▼  │
├─────────────────────────────────────────┤
│ 🤖 Model: anthropic/claude-3.5-sonnet  │
│ 🔌 Provider: openrouter                │
│                                         │
│ 📥 Input Tokens: 150                   │
│ 📤 Output Tokens: 250                  │
│ 📊 Total Tokens: 400                   │
│                                         │
│ 💰 Cost: $0.004200                     │
│ ⏱️ Response Time: 1234ms               │
│                                         │
│ Token Distribution                     │
│ ████████░░░░░░░░░░░░ (37.5% / 62.5%)   │
│ 📥 150 📤 250                           │
└─────────────────────────────────────────┘
```

## 🔧 Technische Details

### Backend-Änderungen
1. **ResponseMetrics.java** - DTO mit Token-, Kosten- und Zeit-Informationen
2. **ChatResponse.java** - Erhält `metrics` Property
3. **OpenRouterToolClient.java** - Neue Methode `requestCompletionWithMetrics()`
4. **AgentService.java** - Sammelt Metriken und gibt sie zurück

### Frontend-Änderungen
1. **MetricsCard.vue** - Neue Komponente für Metriken-Anzeige
2. **ChatInterface.vue** - Integriert MetricsCard und speichert Metriken
3. **types.ts** - ResponseMetrics-Type definiert
4. **chatService.ts** - Unterstützt model-Parameter

## 🎯 Features

✅ **Token-Tracking**: Input, Output, Total  
✅ **Kosten-Berechnung**: Automatisch aus ModelPricingConfig  
✅ **Response-Zeit**: In Millisekunden  
✅ **Model-Info**: Zeigt welches Model verwendet wurde  
✅ **Visuelle Darstellung**: Token-Verteilungs-Balken  
✅ **Collapsible**: Kann ein-/ausgeklappt werden  
✅ **Responsive**: Funktioniert auf allen Geräten  
✅ **Smooth Animations**: Slide-Down Effekt  

## 📈 Datenfluss

```
User sendet Nachricht
         ↓
Backend berechnet Antwort
         ↓
Sammelt Metrics:
  • inputTokens
  • outputTokens
  • cost (berechnet)
  • responseTimeMs
  • model
  • provider
         ↓
ChatResponse mit metrics
         ↓
Frontend speichert Metriken
         ↓
MetricsCard rendert Metriken
```

## 🎨 Design-Highlights

- **Gradient Background**: Blau (#f5f7fa → #c3cfe2)
- **Input-Balken**: Blau (#3498db → #2980b9)
- **Output-Balken**: Grün (#2ecc71 → #27ae60)
- **Cost-Highlight**: Hellgrün Background
- **Hover-Effect**: Erhöhte Schatten + Transform

## 📋 API Response Format

```json
{
  "reply": "Dies ist die KI-Antwort...",
  "toolName": "OpenRouterToolClient",
  "timestamp": "2025-12-09T22:00:00Z",
  "metrics": {
    "inputTokens": 150,
    "outputTokens": 250,
    "totalTokens": 400,
    "cost": 0.00420,
    "responseTimeMs": 1234,
    "model": "anthropic/claude-3.5-sonnet",
    "provider": "openrouter"
  }
}
```

## 💡 Tipps für Benutzer

1. **Kosten vergleichen**: Verschiedene Modelle haben unterschiedliche Preise
   - Gemma 3N: sehr günstig
   - Claude 3.5 Sonnet: gutes Verhältnis
   - Claude Opus: teuer aber sehr mächtig

2. **Token-Limits beachten**: Größere Modelle kosten mehr
   - Input-Tokens: Abhängig von Frage-Länge
   - Output-Tokens: Abhängig von Antwort-Länge

3. **Response-Zeit**: Hilft zu verstehen, wie lange API braucht
   - Typisch: 1-3 Sekunden
   - Abhängig von Modell und Komplexität

4. **Metriken einklappen**: Wenn nicht benötigt, Platz sparen

## 🔍 Debugging

### Console prüfen (F12)
```javascript
// Du siehst:
📊 Metrics stored for message: {
  inputTokens: 150,
  outputTokens: 250,
  totalTokens: 400,
  cost: 0.00420,
  responseTimeMs: 1234,
  model: "anthropic/claude-3.5-sonnet",
  provider: "openrouter"
}
```

### Network prüfen (DevTools → Network)
1. Sende eine Nachricht
2. Klicke auf POST `/api/chat`
3. Response Tab → Siehst JSON mit metrics

## 📝 Beispiel-Szenarios

### Scenario 1: Kurze Frage zu Claude 3.5 Sonnet
```
Input: "Was ist Python?"
Metriken:
  Input: 10 tokens (~0.00003$)
  Output: 100 tokens (~0.0015$)
  Total: ~0.00153$
  Time: 800ms
```

### Scenario 2: Lange Frage zu GPT-4o
```
Input: "Schreib mir einen kompletten Blog-Post über..."
Metriken:
  Input: 500 tokens (~0.0025$)
  Output: 2000 tokens (~0.03$)
  Total: ~0.0325$
  Time: 3500ms
```

### Scenario 3: Günstige Anfrage zu Mistral Small
```
Input: "Hallo!"
Metriken:
  Input: 5 tokens (~0.0000007$)
  Output: 50 tokens (~0.000021$)
  Total: ~0.000022$ (praktisch kostenlos!)
  Time: 400ms
```

## 🚀 Next Steps

1. **Test die Metriken**: Sende verschiedene Nachrichten
2. **Vergleiche Modelle**: Nutze verschiedene OpenRouter Models
3. **Monitoring**: Track deine Gesamtkosten über Zeit
4. **Optimierung**: Wähle günstigere Modelle für einfache Aufgaben

## ❓ FAQs

**F: Warum sind die Metriken manchmal leer?**  
A: Bei Perplexity Provider gibt es noch keine Metriken. Nur bei OpenRouter.

**F: Ist die Cost-Berechnung genau?**  
A: Sie basiert auf ModelPricingConfig. Die API sendet auch eine Cost, die kann leicht abweichen.

**F: Kann ich die Metriken exportieren?**  
A: Noch nicht, aber das ist geplant!

**F: Warum dauert eine Antwort manchmal länger?**  
A: Abhängig von Modell-Komplexität, API-Last und Ihrer Frage-Länge.

## 📚 Weitere Dokumentation

- `MODEL_PRICING_FEATURE.md` - Pricing-System Details
- `MODEL_PRICING_EXAMPLES.md` - Kostenbeispiele für alle Modelle
- `METRICS_DISPLAY_IMPLEMENTATION.md` - Technische Implementierungsdetails

## ✨ Zusammenfassung

🎉 Deine API-Anfragen werden jetzt mit vollständigen Metriken getracked!

Alle wichtigen Informationen sind sichtbar:
- 📊 Tokens: Wie viele wurden verwendet
- 💰 Kosten: Wie viel hat die Anfrage gekostet
- ⏱️ Zeit: Wie lange die API brauchte
- 🤖 Model: Welches Model wurde verwendet

Viel Spaß beim Erkunden!

