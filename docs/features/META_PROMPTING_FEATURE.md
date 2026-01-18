# 🎯 Meta-Prompting Feature - Universal AI Assistant

## 📋 Übersicht

Ein **universeller adaptiver Agent**, der sich dynamisch an BELIEBIGE Ziele anpasst und strukturierte Ergebnisse erstellt.

### Was ist Meta-Prompting?

Meta-Prompting ist ein Prompt, der **sich selbst an jedes Thema anpasst**:
- ✅ Fragt den Nutzer nach seinem Ziel
- ✅ Generiert dynamisch die benötigten Felder
- ✅ Sammelt Informationen durch Dialog
- ✅ Stoppt automatisch wenn vollständig
- ✅ Erstellt strukturiertes Ergebnis im passenden Format

## 🎯 Unterstützte Ziele

Der Meta-Agent kann automatisch bei folgenden Zielen helfen:

### 1. 📋 Menüplanung
**Felder**: family_size, dietary_restrictions, budget, cooking_time, cuisines  
**Output**: Wochenplan + Einkaufsliste + KBJU

### 2. ✈️ Reiseplanung
**Felder**: destination, dates, budget, travelers, interests, accommodation  
**Output**: Tagesplan + Buchungsliste + Budget + Packliste

### 3. 💼 Businessplan
**Felder**: business_idea, target_market, revenue_model, investment, competitors  
**Output**: Executive Summary + Marktanalyse + Finanzplanung

### 4. 📝 Anforderungsdokument (Tech Spec)
**Felder**: project_name, problem, stakeholders, features, tech_stack  
**Output**: Strukturiertes Requirements-Dokument

### 5. 🛒 Kaufentscheidung
**Felder**: product_category, budget, must_haves, use_case  
**Output**: Vergleichstabelle + Empfehlung + Kauflinks

### 6. 💪 Trainingsplan
**Felder**: fitness_level, goals, available_time, equipment  
**Output**: Wochenplan + Progression + Ernährungstipps

### 7. 🏠 Umzugsplanung
**Felder**: locations, move_date, household_size, budget, services  
**Output**: Checkliste + Timeline + Anbieter-Empfehlungen

### 8. 🎓 Lernplan
**Felder**: subject, current_level, goal, time, learning_style  
**Output**: Studienplan + Ressourcen + Meilensteine

### 9. ❓ BELIEBIGES ZIEL
Der Agent kann sich an **jedes neue Ziel** anpassen und sinnvolle Felder ableiten!

## 🏗️ Architektur

### Backend (Java/Spring Boot)

```
service/strategy/
├── MetaPromptStrategy.java       ✨ NEU - Universal Adapter
├── NutritionistStrategy.java     (spezifisch für Ernährung)
├── CustomSchemaInstructionStrategy.java
├── SimpleJsonInstructionStrategy.java
└── AutoSchemaInstructionStrategy.java
```

### Frontend (Vue 3)

```
components/
└── MetaPromptChat.vue            ✨ NEU - Universal UI
```

## 📊 JSON State Tracking

Der Agent gibt IMMER ein strukturiertes JSON zurück:

### Phase: "init" oder "collecting"

```json
{
  "phase": "collecting",
  "goal_type": "Reiseplanung",
  "fields_total": 8,
  "fields_collected": 3,
  "completion_percentage": 37,
  "missing_fields": ["budget", "interests", "accommodation_type"],
  "assistant_message": "Super, Paris im Sommer! Wie viel Budget habt ihr eingeplant?",
  "collected_data": {
    "destination": "Paris",
    "duration": "7 Tage",
    "travelers_count": 2
  }
}
```

### Phase: "complete"

```json
{
  "phase": "complete",
  "goal_type": "Reiseplanung",
  "fields_total": 8,
  "fields_collected": 8,
  "completion_percentage": 100,
  "summary": "7-tägige Paris-Reise für 2 Personen - inkl. Tagesplan und Tipps",
  "final_output": "# 🗼 PARIS REISEPLAN\n\n## 📅 Übersicht\n- **Reiseziel**: Paris\n..."
}
```

## 🚀 Verwendung

### API Request

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Ich möchte eine Reise planen",
    "userId": "user123",
    "conversationId": "conv_meta_1",
    "jsonMode": true,
    "jsonSchema": "meta_prompt"
  }'
```

### Frontend Integration

```typescript
// In deiner Vue-Komponente
const response = await chatService.sendMessage({
  message: userMessage,
  jsonMode: true,
  jsonSchema: 'meta_prompt'  // ← Der Marker!
});

const data = JSON.parse(response.response);

if (data.phase === 'complete') {
  // Zeige finales Ergebnis
  displayFinalOutput(data.final_output);
} else {
  // Zeige Chat-Nachricht
  displayMessage(data.assistant_message);
  // Update Progress Bar
  updateProgress(data.completion_percentage);
}
```

## 🎨 UI Features

### 1. **Dynamic Progress Bar**
- Zeigt Fortschritt in Echtzeit (0-100%)
- Farbcodiert: Rot → Gelb → Grün
- Zeigt gesammelte vs. fehlende Felder

### 2. **Missing Fields Chips**
- Visualisiert noch benötigte Informationen
- Nur bei < 5 fehlenden Feldern sichtbar

### 3. **Force Complete Button**
- Erscheint ab 70% Completion
- Nutzer kann vorzeitig abschließen

### 4. **Markdown Rendering**
- Finales Ergebnis wird als formatierter Markdown angezeigt
- Unterstützt Tabellen, Listen, Überschriften

### 5. **Export Optionen**
- Kopieren in Zwischenablage
- Download als Markdown-Datei
- (Optional) PDF-Export

## 🎯 Beispiel-Dialoge

### Beispiel 1: Reiseplanung

```
User: Ich möchte eine Reise planen
Assistant: Hi! Was möchtest du heute erstellen oder planen?
          (Zeigt Beispiele: Businessplan, Reiseplan, etc.)

User: Eine Reise nach Paris
Assistant: Super! Wann möchtest du reisen und für wie lange?

User: Im Sommer, 7 Tage, wir sind 2 Personen
Assistant: Klingt toll! Wie viel Budget habt ihr ungefähr eingeplant?
          Und welche Art von Unterkunft bevorzugt ihr?

User: 1500€, Hotel im Zentrum
Assistant: Perfekt! Was sind eure Haupt-Interessen in Paris?
          (Kultur, Shopping, Kulinarik, Nightlife, etc.)

User: Kultur und gutes Essen
Assistant: [Phase: complete]
          
          # 🗼 PARIS REISEPLAN
          
          ## 📅 TAGESPLAN
          ### Tag 1: Ankunft & Quartier Latin
          ...
          
          ## 🏨 BUCHUNGSLISTE
          ...
```

### Beispiel 2: Kaufentscheidung

```
User: Ich brauche Hilfe bei einer Kaufentscheidung
Assistant: Gerne! Was möchtest du kaufen?

User: Einen neuen Laptop für Programmierung
Assistant: Alles klar! Was ist dein Budget-Rahmen?

User: Bis 1500€
Assistant: Welche Features sind dir am wichtigsten?
          (z.B. Performance, Akkulaufzeit, Display-Qualität, Mobilität)

User: Performance und gutes Display
Assistant: [Phase: complete]
          
          # 💻 LAPTOP-KAUFEMPFEHLUNG
          
          ## 📊 TOP 3 VERGLEICH
          ...
          
          ## 🏆 EMPFEHLUNG
          ...
```

## ⚙️ Konfiguration

### System-Prompt anpassen

```java
// MetaPromptStrategy.java
private String buildMetaSystemPrompt() {
    return """
        // Dein angepasster Meta-Prompt hier
        """;
}
```

### Neue Ziel-Typen hinzufügen

```java
// Im System-Prompt ergänzen:
🎮 GAMING-SETUP:
  → budget, game_types, platform, room_space, 
     performance_goals, peripherals
```

## 🧪 Testing

### Test-Skript erstellen

```bash
#!/bin/bash
# test-meta-prompt.sh

curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Ich möchte einen Businessplan erstellen",
    "userId": "test_user",
    "conversationId": "test_meta",
    "jsonMode": true,
    "jsonSchema": "meta_prompt"
  }' | jq '.'
```

## 🔍 Debugging

### Logs prüfen

```bash
tail -f backend/perplexity-service/logs/app.log | grep -i "meta"
```

### Strategy-Auswahl verifizieren

```bash
# Sollte zeigen:
✅ Added JSON mode instruction (custom-schema: meta_prompt)
```

## 💡 Erweiterte Use-Cases

### 1. **Hochzeitsplanung**
- Felder: Budget, Gästeanzahl, Location-Typ, Datum, Stil
- Output: Timeline + Vendor-Liste + Budget-Breakdown

### 2. **Ernährungsumstellung**
- Felder: Aktuell, Ziel, Zeitrahmen, Einschränkungen
- Output: Wochenplan + Rezepte + Einkaufsliste

### 3. **Karriereplanung**
- Felder: Aktueller Job, Ziel, Skills, Timeline
- Output: Lernplan + Job-Strategie + Networking-Tipps

### 4. **App-Entwicklung**
- Felder: App-Idee, Zielgruppe, Features, Budget
- Output: Tech-Stack + Roadmap + MVP-Definition

## ⚠️ Wichtige Hinweise

### Completion-Trigger

Der Agent stoppt automatisch wenn:
- ✅ `completion_percentage >= 90%`
- ✅ User sagt "fertig", "genug", "erstelle jetzt"
- ✅ Alle kritischen Felder erfüllt

### State Tracking

Der Agent führt **intern** eine Checkliste:
- User sieht diese NICHT im Chat
- Nur in der JSON-Response für das Frontend
- Ermöglicht intelligentes Progress-Tracking

### Output-Format

Das finale Ergebnis:
- Ist IMMER in `final_output` als Markdown
- Wird vom Frontend mit `marked` gerendert
- Kann kopiert oder heruntergeladen werden

## 🎓 Best Practices

### 1. **Sei spezifisch beim Start**

❌ "Ich brauche Hilfe"  
✅ "Ich möchte einen Businessplan erstellen"

### 2. **Gib mehrere Infos auf einmal**

❌ "Paris" → "7 Tage" → "2 Personen"  
✅ "Eine Woche Paris für 2 Personen"

### 3. **Nutze Force Complete**

Wenn du mit 70-90% der Felder zufrieden bist, klicke "✅ Fertigstellen"

## 🚀 Deployment

```bash
# Backend starten
cd backend/perplexity-service
mvn spring-boot:run

# Frontend starten
cd frontend
npm install marked  # Falls noch nicht installiert
npm run dev
```

## 📊 Strategy-Prioritäten

1. **MetaPromptStrategy** (wenn `jsonSchema` = "meta_prompt")
2. **NutritionistStrategy** (wenn `jsonSchema` = "nutritionist_mode")
3. **AutoSchemaInstructionStrategy** (wenn `autoSchema` = true)
4. **CustomSchemaInstructionStrategy** (wenn `jsonSchema` gesetzt)
5. **SimpleJsonInstructionStrategy** (Fallback)

## 🎉 Erfolgsmetriken

Nach Implementierung kannst du:
- ✅ **Beliebige Ziele** vom Agent bearbeiten lassen
- ✅ **Automatische Feldgenerierung** nutzen
- ✅ **Strukturierte Outputs** für verschiedene Domains erhalten
- ✅ **Ein UI** für alle Use-Cases verwenden
- ✅ **Progress Tracking** in Echtzeit

---

**Created**: 2025-12-03  
**Version**: 1.0.0  
**Author**: AI_Advent_Challenge Team

