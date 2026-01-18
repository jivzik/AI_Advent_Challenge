# Nutritionist Agent Feature - Familienernährungsberater

## 📋 Übersicht

Ein intelligenter Conversational Agent, der durch Dialog alle notwendigen Informationen sammelt und automatisch ein vollständiges Wochenmenü mit KBJU-Berechnungen und Shopping-Liste für Familien in Deutschland erstellt.

## 🎯 Ziel der Aufgabe

**Задать ограничение модели, чтобы она сама остановилась**
- Die KI sammelt Informationen durch Dialog
- Sobald alle erforderlichen Daten vollständig sind, stoppt die KI automatisch
- Sie generiert ein strukturiertes Ergebnis (Wochenmenü + Einkaufsliste)

## 🏗️ Implementierung

### Backend-Architektur

```
service/strategy/
├── JsonInstructionStrategy.java          # Interface
├── NutritionistStrategy.java             # ✨ NEUE Strategie
├── CustomSchemaInstructionStrategy.java  # Angepasst (Priorität)
├── SimpleJsonInstructionStrategy.java    # Angepasst (Fallback)
└── AutoSchemaInstructionStrategy.java    # Unverändert
```

### Wie es funktioniert

1. **Strategy Pattern**: Automatische Injection aller Strategies via Spring
2. **Prioritäts-System**:
   - `NutritionistStrategy` → Höchste Priorität (wenn `jsonSchema` = "nutritionist_mode")
   - `AutoSchemaInstructionStrategy` → Hoch (wenn `autoSchema` = true)
   - `CustomSchemaInstructionStrategy` → Mittel (wenn `jsonSchema` gesetzt, aber keine Special-Mode)
   - `SimpleJsonInstructionStrategy` → Niedrigste (Fallback für basic JSON mode)

3. **Dialog-Steuerung**:
   - Agent stellt 1-2 Fragen pro Nachricht
   - Sammelt alle Pflichtdaten (Familie, Allergien, Budget, etc.)
   - Sobald alle Daten vollständig → generiert finales JSON-Ergebnis

## 📊 Datenfluss

```mermaid
User Request (jsonSchema: "nutritionist_mode")
    ↓
MessageHistoryManager
    ↓
NutritionistStrategy.canHandle() → true
    ↓
buildInstruction() → System-Prompt + JSON-Schema
    ↓
Perplexity API (mit vollständigem Context)
    ↓
Response:
  - Status: "collecting" → Weiterer Dialog
  - Status: "complete" → Vollständiges Wochenmenü
```

## 🔧 API-Nutzung

### Request-Format

```json
POST /api/chat

{
  "message": "Ich brauche Hilfe bei der Wochenplanung für meine Familie",
  "userId": "user123",
  "conversationId": "conv456",
  "jsonMode": true,
  "jsonSchema": "nutritionist_mode"
}
```

### Response während Datensammlung

```json
{
  "response": "{\"status\": \"collecting\", \"response\": \"Hallo! Gerne helfe ich dir bei der Wochenplanung. Lass uns mit deiner Familie starten. Wie viele Personen seid ihr und wie alt sind sie?\", \"collected_data\": {}, \"missing_data\": [\"family_members\", \"allergies\", \"budget\", \"...\"]}",
  "conversationId": "conv456",
  "timestamp": "2025-12-03T10:30:00Z"
}
```

### Finales Response (wenn alle Daten gesammelt)

```json
{
  "response": "{\"status\": \"complete\", \"family_profile\": {...}, \"weekly_menu\": [...], \"shopping_list\": {...}, \"meal_prep_tips\": [...]}",
  "conversationId": "conv456",
  "timestamp": "2025-12-03T10:45:00Z"
}
```

## 📝 Gesammelte Daten (Checkliste)

### Pflichtfelder

- [x] **Familie**: Mitglieder, Alter, Gewicht, Größe, Aktivitätslevel
- [x] **Gesundheit**: Allergien, Striktheit, Diät-Typ, Gesundheitsziele
- [x] **Präferenzen**: Likes, Dislikes, Küchen, Schärfe-Level
- [x] **Praktisches**: Budget, Kochzeit, Batch-Cooking, Mahlzeiten
- [x] **Einkauf**: Bevorzugte Läden, Einkaufsfrequenz
- [x] **Ausstattung**: Verfügbare Küchengeräte

## 🧮 KBJU-Berechnung

Das System berechnet automatisch für jedes Familienmitglied:

### BMR (Basal Metabolic Rate) nach Mifflin-St Jeor
- **Männer**: (10 × Gewicht_kg) + (6.25 × Größe_cm) − (5 × Alter) + 5
- **Frauen**: (10 × Gewicht_kg) + (6.25 × Größe_cm) − (5 × Alter) − 161
- **Kinder 4-10**: ~1200-1600 kcal
- **Teenager**: ~1800-2400 kcal

### TDEE (Total Daily Energy Expenditure)
TDEE = BMR × Aktivitätsfaktor:
- Sitzend: 1.2
- Leichte Aktivität: 1.375
- Mittlere Aktivität: 1.55
- Hohe Aktivität: 1.725

### Makronährstoff-Verteilung
- **Proteine**: 25-30% (1.2-2g/kg) — 4 kcal/g
- **Fette**: 25-30% (0.8-1.2g/kg) — 9 kcal/g
- **Kohlenhydrate**: 40-50% (2-4g/kg) — 4 kcal/g

## 🛒 Deutsche Supermarkt-Kenntnisse

### Glutenfreie Produkte
- **DM, Rossmann**: Beste Auswahl (Schär, etc.)
- **REWE**: "REWE Frei Von" Linie
- **Edeka, Kaufland**: Gute GF-Regale
- **Lidl**: Begrenzte Auswahl
- **Aldi**: Fast keine GF-Produkte

### Preiskategorien
- **Günstig**: Lidl, Aldi, Netto, Penny
- **Mittel**: REWE, Edeka
- **Premium**: Alnatura, Bio Company, denn's

## 🎨 Frontend-Integration (Vorschlag)

```typescript
// services/nutritionistService.ts
export async function startNutritionistSession(message: string) {
  return chatService.sendMessage({
    message,
    jsonMode: true,
    jsonSchema: 'nutritionist_mode'
  });
}

// Komponente mit Status-Anzeige
interface NutritionistResponse {
  status: 'collecting' | 'complete';
  response?: string;
  collected_data?: any;
  missing_data?: string[];
  weekly_menu?: any;
  shopping_list?: any;
}
```

## 🧪 Testing

### Testfall 1: Normaler Dialog-Flow

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Ich brauche einen Ernährungsplan",
    "userId": "test_user",
    "conversationId": "test_conv_1",
    "jsonMode": true,
    "jsonSchema": "nutritionist_mode"
  }'
```

### Testfall 2: Schrittweiser Dialog

1. **Nachricht 1**: "Ich brauche Hilfe für meine Familie"
2. **Nachricht 2**: "Wir sind 3: Ich (35, 85kg, 180cm), meine Frau (33, 65kg, 165cm) und unser Kind (6 Jahre)"
3. **Nachricht 3**: "Unser Kind hat Glutenunverträglichkeit, sehr streng"
4. **Nachricht 4**: "Budget 80€, REWE und DM, 30 Min Kochzeit"
5. → System generiert vollständiges Menü

## 📈 Erwartetes Ergebnis

Nach vollständiger Datensammlung erhält der Nutzer:

✅ **Wochenmenü** (7 Tage)
- Frühstück, Mittagessen, Abendessen
- KBJU pro Portion (für jedes Familienmitglied)
- Detaillierte Zutatenlisten
- Schritt-für-Schritt Anleitungen
- GF-Varianten (wo nötig)

✅ **Shopping-Liste**
- Aufgeteilt nach Supermärkten (Lidl/REWE/DM)
- Mit Preisen und Mengen
- Gesamt-Budget-Übersicht

✅ **Meal-Prep-Tips**
- Batch-Cooking Vorschläge
- GF-Sicherheits-Hinweise
- Aufbewahrungstipps

## 🚀 Deployment

```bash
# Backend neu kompilieren
cd backend/perplexity-service
mvn clean install

# Service starten
./start-backend.sh

# Frontend (falls UI-Update nötig)
cd frontend
npm run build
```

## 🔍 Debugging

### Logs prüfen
```bash
# Spring Boot Logs
tail -f backend/perplexity-service/logs/app.log | grep NutritionistStrategy
```

### Strategy-Auswahl testen
```java
// In MessageHistoryManager.java wird geloggt:
log.info("✅ Added JSON mode instruction (auto-schema: {}, custom-schema: {})", ...)
```

## ⚡ Performance-Optimierung

1. **Caching**: Häufige Produktpreise cachen
2. **Batch-Processing**: Mehrere Tage gleichzeitig berechnen
3. **Streaming**: Für lange Menüs Response streamen

## 📚 Weiterführende Ideen

- [ ] **PDF-Export**: Menü als druckbare PDF
- [ ] **Kalender-Integration**: Menü in Google Calendar
- [ ] **Shopping-List-App**: Integration mit Bringmeister/REWE Lieferservice
- [ ] **Rezept-Fotos**: KI-generierte Bilder der Gerichte
- [ ] **Allergiker-Datenbank**: Erweiterte Kreuzallergie-Checks

## 🐛 Bekannte Einschränkungen

- Modell könnte manchmal zu früh stoppen (wenn es denkt, genug Daten zu haben)
- KBJU-Berechnungen sind Näherungswerte
- Preise können je nach Region variieren
- GF-Verfügbarkeit kann sich ändern

## 🤝 Contribution

Erweiterungen willkommen:
- Weitere Special-Diets (Halal, Kosher, etc.)
- Andere Länder/Supermärkte
- Saisonale Anpassungen
- Fitness-Tracking-Integration

---

**Erstellt**: 2025-12-03  
**Version**: 1.0.0  
**Autor**: AI_Advent_Challenge Team

