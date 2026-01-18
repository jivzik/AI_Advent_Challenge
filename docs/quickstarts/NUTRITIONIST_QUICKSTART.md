# 🍽️ Nutritionist Agent - Schnellstart

## Was ist das?

Ein Conversational AI Agent, der:
1. ✅ Durch Dialog Informationen sammelt
2. ✅ Automatisch stoppt, wenn alle Daten vollständig sind
3. ✅ Ein vollständiges Wochenmenü mit KBJU und Shopping-Liste generiert

## 🚀 Schnellstart

### 1. Backend starten

```bash
cd /home/jivz/IdeaProjects/AI_Advent_Challenge
./start-backend.sh
```

### 2. Test ausführen

```bash
./test-nutritionist.sh
```

### 3. Manueller API-Test

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Ich brauche einen Ernährungsplan für meine Familie",
    "userId": "test_user",
    "conversationId": "test_conv_1",
    "jsonMode": true,
    "jsonSchema": "nutritionist_mode"
  }'
```

## 🎯 Wichtiger Marker

Um den Nutritionist-Modus zu aktivieren, MUSS das Feld `jsonSchema` den Wert enthalten:

```json
"jsonSchema": "nutritionist_mode"
```

## 📋 Beispiel-Dialog

### Nachricht 1 (User)
```
Hallo! Ich brauche Hilfe bei der Essensplanung für meine Familie.
```

**Response (Status: collecting)**
```json
{
  "status": "collecting",
  "response": "Hallo! Gerne helfe ich dir bei der Wochenplanung. Lass uns mit deiner Familie starten. Wie viele Personen seid ihr und wie alt sind sie?",
  "missing_data": ["family_members", "allergies", "budget", "..."]
}
```

### Nachricht 2 (User)
```
Wir sind 3: Papa (35, 85kg, 180cm), Mama (33, 65kg, 165cm), Kind (6 Jahre)
```

**Response (Status: collecting)**
```json
{
  "status": "collecting",
  "response": "Super! Jetzt zu den Gesundheitsaspekten: Gibt es Allergien oder Unverträglichkeiten in der Familie?",
  "collected_data": {
    "family_members": [...]
  },
  "missing_data": ["allergies", "budget", "..."]
}
```

### ... weitere Schritte ...

### Letzte Nachricht (wenn alle Daten vollständig)

**Response (Status: complete)**
```json
{
  "status": "complete",
  "family_profile": {
    "members": [...],
    "restrictions": {...},
    "preferences": {...}
  },
  "weekly_menu": [
    {
      "day": "Montag",
      "meals": [
        {
          "type": "breakfast",
          "name": "Haferflocken mit Beeren",
          "nutrition_per_serving": {
            "adult": {"calories": 350, "protein": 12, "fat": 14, "carbs": 45},
            "child": {"calories": 250, "protein": 8, "fat": 10, "carbs": 32}
          },
          "ingredients": [...],
          "instructions": [...]
        }
      ]
    }
  ],
  "shopping_list": {
    "Lidl": {...},
    "REWE": {...},
    "DM": {...},
    "total_budget": 72.29
  },
  "meal_prep_tips": [...],
  "gf_safety_tips": [...]
}
```

## 🧪 Vollständiger Test-Flow

Der `test-nutritionist.sh` Script simuliert einen kompletten Dialog:

1. Begrüßung
2. Familieninformationen (Alter, Gewicht, Größe)
3. Aktivitätslevel
4. Allergien (z.B. Glutenunverträglichkeit)
5. Gesundheitsziele (abnehmen, halten, zunehmen)
6. Präferenzen (Lieblingsessen, Abneigungen)
7. Budget und Kochzeit
8. Bevorzugte Supermärkte
9. Küchenausstattung

→ **Erwartetes Ergebnis**: Vollständiges Wochenmenü im JSON-Format

## 📁 Ausgabe

Das Ergebnis wird gespeichert in:
```
nutritionist_result_<timestamp>.json
```

Diese Datei enthält das komplette Wochenmenü und kann:
- Als PDF exportiert werden
- In eine App importiert werden
- Direkt ausgedruckt werden

## 🔍 Debugging

### Backend-Logs prüfen

```bash
# In einem separaten Terminal
tail -f backend/perplexity-service/logs/app.log | grep -i nutritionist
```

### Strategy-Auswahl verifizieren

```bash
# Suche nach diesem Log-Eintrag:
grep "Added JSON mode instruction" backend/perplexity-service/logs/app.log
```

Erwartete Ausgabe:
```
✅ Added JSON mode instruction (auto-schema: false, custom-schema: nutritionist_mode)
```

## 💡 Erweiterte Nutzung

### Frontend-Integration

```typescript
// In deiner Chat-Komponente
const startNutritionistMode = async () => {
  const response = await chatService.sendMessage({
    message: "Ich brauche einen Ernährungsplan",
    jsonMode: true,
    jsonSchema: 'nutritionist_mode'
  });
  
  const data = JSON.parse(response.response);
  
  if (data.status === 'complete') {
    // Zeige Wochenmenü an
    displayWeeklyMenu(data.weekly_menu);
    displayShoppingList(data.shopping_list);
  } else {
    // Zeige nächste Frage
    displayMessage(data.response);
  }
};
```

### Custom Frontend (Beispiel)

```vue
<template>
  <div class="nutritionist-chat">
    <div v-if="isCollecting">
      <ChatMessages :messages="messages" />
      <ProgressBar :collected="collectedData" :missing="missingData" />
    </div>
    
    <div v-if="isComplete">
      <WeeklyMenuView :menu="weeklyMenu" />
      <ShoppingListView :list="shoppingList" />
      <button @click="exportPDF">Als PDF exportieren</button>
    </div>
  </div>
</template>
```

## 🎨 UI-Verbesserungen (optional)

1. **Progress Bar**: Zeige welche Daten noch fehlen
2. **Checkboxen**: Visualisiere gesammelte Informationen
3. **Vorschau**: Zeige zwischendurch KBJU-Berechnungen
4. **Export**: PDF, iCal, Shopping-List-App

## ⚙️ Konfiguration

### System-Prompt anpassen

Falls du den System-Prompt ändern möchtest:

```java
// NutritionistStrategy.java
private String buildSystemPrompt() {
    return """
        Dein angepasster System-Prompt hier...
        """;
}
```

### JSON-Schema erweitern

```java
private String buildOutputSchema() {
    return """
        Dein angepasstes JSON-Schema hier...
        """;
}
```

## 🐛 Troubleshooting

### Problem: Agent stoppt nicht automatisch

**Lösung**: 
- Prüfe, ob alle Pflichtdaten in der Checkliste vorhanden sind
- Erhöhe die "temperature" in der Perplexity-Config
- Füge explizite Stop-Bedingung im Prompt hinzu

### Problem: Response ist kein valides JSON

**Lösung**:
- Prüfe `JsonResponseParser.java`
- Aktiviere Debug-Logs für Perplexity-Response
- Verwende `jsonMode: true` im Request

### Problem: Strategy wird nicht ausgewählt

**Lösung**:
```bash
# Prüfe, ob der Marker richtig gesetzt ist
grep "nutritionist_mode" backend/perplexity-service/logs/app.log

# Stelle sicher, dass NutritionistStrategy.canHandle() true zurückgibt
```

## 📚 Weitere Dokumentation

- Vollständige Dokumentation: `NUTRITIONIST_AGENT_FEATURE.md`
- API-Referenz: `backend/perplexity-service/README.md`
- Frontend-Integration: `frontend/README.md`

## 🎓 Lernen & Experimentieren

### Andere Use-Cases

Das gleiche Pattern kann verwendet werden für:

1. **Reiseplaner**: Sammle Ziel, Budget, Interessen → Generiere Itinerary
2. **Fitness-Coach**: Sammle Fitness-Level, Ziele → Generiere Trainingsplan
3. **Haushaltsbudget**: Sammle Einnahmen, Ausgaben → Generiere Finanzplan
4. **Lernplan**: Sammle Ziele, verfügbare Zeit → Generiere Lernplan

### Neue Strategy erstellen

1. Erstelle neue Strategy-Klasse
2. Implementiere `JsonInstructionStrategy`
3. Definiere Marker (z.B. `"travel_planner_mode"`)
4. Implementiere `canHandle()` und `buildInstruction()`
5. Spring lädt automatisch die neue Strategy!

---

**Viel Erfolg! 🚀**

