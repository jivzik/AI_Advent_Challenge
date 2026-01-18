# Temperature Feature - Schnellstart

## 🚀 Sofort loslegen

Das Temperature-Feature ist jetzt verfügbar! Hier ist alles, was Sie wissen müssen:

## Was ist neu?

Ein **Temperature-Slider** im Chat-Interface ermöglicht es Ihnen, die Kreativität der KI-Antworten zu steuern.

## Wo finde ich es?

Der Temperature-Slider befindet sich direkt unter dem **System Prompt**-Bereich im Chat-Interface.

## Wie benutze ich es?

### Schritt 1: Anwendung starten

```bash
# Terminal 1 - Backend starten
cd backend/perplexity-service
./mvnw spring-boot:run

# Terminal 2 - Frontend starten  
cd frontend
npm run dev
```

### Schritt 2: Temperature einstellen

1. Öffnen Sie den Chat im Browser (http://localhost:5173)
2. Finden Sie den **🌡️ Temperature** Slider unter dem System Prompt
3. Verschieben Sie den Slider nach links oder rechts:
   - **Links (0.0)**: Präzise, faktische Antworten
   - **Mitte (1.0)**: Ausgewogene Antworten
   - **Rechts (2.0)**: Kreative, experimentelle Antworten

### Schritt 3: Testen Sie es!

Probieren Sie denselben Prompt mit verschiedenen Temperature-Werten:

#### Beispiel: "Erkläre Quantenphysik"

- **Temperature 0.1**: Kurze, präzise Definition
- **Temperature 0.7**: Ausgewogene Erklärung mit Beispielen
- **Temperature 1.5**: Kreative Analogien und Metaphern

## 📊 Temperature-Empfehlungen

| Temperature | Anwendungsfall | Beispiel |
|-------------|----------------|----------|
| 0.0 - 0.3 | Fakten, Code, Mathematik | "Was ist 2+2?", "Schreibe eine Funktion..." |
| 0.4 - 0.9 | Konversation, Erklärungen | "Wie funktioniert...?", "Gib mir Tipps..." |
| 1.0 - 2.0 | Kreatives Schreiben, Brainstorming | "Schreibe eine Geschichte...", "Erfinde..." |

## ⚙️ Technische Details

- **Standardwert**: 0.7 (ausgewogene Kreativität)
- **Bereich**: 0.0 - 2.0
- **Schrittweite**: 0.1
- **Persistenz**: Wird bei Seiten-Reload zurückgesetzt

## 💡 Tipps

1. **Niedrige Temperature für Code**: Wenn Sie Code generieren lassen, nutzen Sie 0.1-0.3 für konsistente Ergebnisse
2. **Hohe Temperature für Ideen**: Für Brainstorming nutzen Sie 1.2-1.8 für mehr Variation
3. **Mittlere Temperature als Standard**: 0.7 ist ein guter Allround-Wert
4. **Vorsicht bei 2.0**: Sehr hohe Werte können zu inkohärenten Antworten führen

## 🐛 Fehlerbehebung

### Slider wird nicht angezeigt
- Stellen Sie sicher, dass Frontend und Backend neu gebaut wurden
- Leeren Sie den Browser-Cache (Ctrl+Shift+R)

### Keine Auswirkung auf Antworten
- Überprüfen Sie die Browser-Konsole auf Fehler
- Stellen Sie sicher, dass das Backend läuft

### Build-Fehler
```bash
# Backend neu bauen
cd backend/perplexity-service
mvn clean install -DskipTests

# Frontend neu bauen
cd frontend
npm install
npm run build
```

## 📝 Weitere Informationen

Vollständige Dokumentation: [TEMPERATURE_FEATURE.md](./TEMPERATURE_FEATURE.md)

## ✅ Checkliste

- [x] Backend kompiliert erfolgreich
- [x] Frontend baut erfolgreich
- [x] Slider im UI sichtbar
- [x] Temperature wird an API gesendet
- [x] Perplexity API erhält Temperature-Parameter

**Status**: ✨ Vollständig implementiert und getestet!

