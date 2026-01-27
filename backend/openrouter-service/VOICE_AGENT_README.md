# Voice Agent - Sprachverarbeitung mit Google Gemini Flash und OpenRouter

## Überblick

Der Voice Agent ist ein vollständiger Pipeline für Sprachverarbeitung:
1. **Audio-Aufnahme** → Benutzer sendet Audiodatei
2. **Speech-to-Text** → Google Gemini Flash transkribiert Audio (via OpenRouter)
3. **LLM-Verarbeitung** → OpenRouter/Claude verarbeitet Text
4. **Text-Antwort** → System gibt Antwort zurück

## Architektur

```
┌─────────────┐
│ Audio Input │
└──────┬──────┘
       │
       v
┌────────────────────┐
│ Google Gemini      │  (Speech-to-Text via OpenRouter)
│ Flash 1.5-8b       │
└────────┬───────────┘
         │
         v
┌──────────────────┐
│ Transcription    │
│ Text             │
└────────┬─────────┘
         │
         v
┌────────────────┐
│ LLM Processing │  (OpenRouter - Claude/etc)
└────────┬───────┘
         │
         v
┌────────────────┐
│ Text Response  │
└────────────────┘
```

## Wichtig: Ein API-Key für alles!

**Nur OpenRouter API-Key benötigt!**
- Audio-Transkription: Google Gemini Flash (via OpenRouter)
- LLM-Processing: Claude/GPT/etc (via OpenRouter)

Kein separater OpenAI API-Key mehr nötig!

## API Endpoints

### 1. `/api/voice/process` - Vollständiger Voice Agent
Verarbeitet Audio durch die gesamte Pipeline.

**Request:**
- Method: `POST`
- Content-Type: `multipart/form-data`
- Parameters:
  - `audio` (required): Audio-Datei (mp3, wav, webm, m4a, etc.)
  - `userId` (required): User ID
  - `language` (optional): Sprache (ISO-639-1: de, en, etc.)
  - `model` (optional): LLM-Modell
  - `temperature` (optional): LLM-Temperatur (0-1)
  - `systemPrompt` (optional): System-Prompt für LLM

**Response:**
```json
{
  "transcription": "Wie ist das Wetter heute?",
  "response": "Ich kann das aktuelle Wetter nicht abrufen...",
  "timestamp": "2026-01-27T10:30:00",
  "language": "de",
  "transcriptionTimeMs": 1200,
  "llmProcessingTimeMs": 3400,
  "totalTimeMs": 4600,
  "model": "anthropic/claude-3.5-sonnet",
  "userId": "user123"
}
```

### 2. `/api/voice/transcribe` - Nur Transkription
Nur Speech-to-Text ohne LLM (für Debugging).

**Request:**
- Method: `POST`
- Content-Type: `multipart/form-data`
- Parameters:
  - `audio` (required): Audio-Datei
  - `language` (optional): Sprache

**Response:**
```json
{
  "text": "Wie ist das Wetter heute?",
  "language": "de",
  "duration": 2.5,
  "model": "whisper-1"
}
```

### 3. `/api/voice/health` - Health Check
Prüft Service-Verfügbarkeit.

## Konfiguration

In `application.properties`:

```properties
# OpenRouter Configuration (für beides: Audio + LLM)
spring.ai.openrouter.api-key=${OPENROUTER_API_KEY}
spring.ai.openrouter.base-url=https://openrouter.ai/api/v1

# Audio Transcription Configuration (Google Gemini Flash)
spring.ai.openrouter.transcription-model=google/gemini-flash-1.5-8b
spring.ai.openrouter.transcription-api-url=https://openrouter.ai/api/v1/chat/completions

# Multipart File Upload Configuration
spring.servlet.multipart.max-file-size=25MB
spring.servlet.multipart.max-request-size=25MB
```

## Umgebungsvariablen

```bash
# Nur ein API-Key benötigt!
export OPENROUTER_API_KEY="sk-or-v1-..."
```

## Unterstützte Audio-Formate

- MP3 (`.mp3`)
- MP4 Audio (`.mp4`)
- MPEG (`.mpeg`)
- MPGA (`.mpga`)
- M4A (`.m4a`)
- WAV (`.wav`)
- WebM (`.webm`)

## Installation und Start

### 1. Dependencies installieren
```bash
cd backend/openrouter-service
mvn clean install
```

### 2. Service starten
```bash
mvn spring-boot:run
```

### 3. Swagger UI öffnen
```
http://localhost:8084/swagger-ui.html
```

## Testing

### Test mit cURL

#### 1. Vollständiger Voice Agent Test
```bash
curl -X POST http://localhost:8084/api/voice/process \
  -F "audio=@test-audio.mp3" \
  -F "userId=test-user-123" \
  -F "language=de" \
  -F "systemPrompt=Du bist ein hilfreicher Assistent."
```

#### 2. Nur Transkription
```bash
curl -X POST http://localhost:8084/api/voice/transcribe \
  -F "audio=@test-audio.mp3" \
  -F "language=de"
```

#### 3. Health Check
```bash
curl http://localhost:8084/api/voice/health
```

### Test mit Python

```python
import requests

# Audio-Datei laden
with open('test-audio.mp3', 'rb') as audio_file:
    files = {'audio': audio_file}
    data = {
        'userId': 'test-user-123',
        'language': 'de',
        'model': 'anthropic/claude-3.5-sonnet',
        'temperature': 0.7
    }
    
    response = requests.post(
        'http://localhost:8084/api/voice/process',
        files=files,
        data=data
    )
    
    print(response.json())
```

## Komponenten

### DTO-Klassen
- `WhisperRequest` - Whisper API Request
- `WhisperResponse` - Whisper API Response
- `VoiceAgentRequest` - Voice Agent Request
- `VoiceAgentResponse` - Voice Agent Response

### Services
- `VoiceAgentService` - Hauptlogik der Voice-Pipeline
  - `processVoiceCommand()` - Vollständige Pipeline
  - `transcribeAudio()` - Nur Transkription

### Controller
- `VoiceAgentController` - REST API Endpoints

### Configuration
- `OpenRouterProperties` - Konfiguration (inkl. Whisper)

## Performance

Typische Response-Zeiten:
- **Transkription (Whisper)**: 1-3 Sekunden
- **LLM-Verarbeitung**: 2-5 Sekunden
- **Gesamt**: 3-8 Sekunden

Abhängig von:
- Audio-Länge
- Audio-Qualität
- Netzwerk-Latenz
- LLM-Modell
- Antwort-Länge

## Fehlerbehandlung

### Häufige Fehler

1. **"Audio file is empty"**
   - Lösung: Prüfe, ob die Datei korrekt hochgeladen wird

2. **"Unsupported audio format"**
   - Lösung: Verwende unterstützte Formate (mp3, wav, webm, etc.)

3. **"Whisper API error"**
   - Lösung: Prüfe OPENAI_API_KEY und API-Guthaben

4. **"Empty response from Whisper API"**
   - Lösung: Audio könnte zu leise oder beschädigt sein

## Logging

Das System loggt ausführlich:
```
INFO - Starting voice agent pipeline for user: test-user-123
INFO - Transcribing audio file: test.mp3 (size: 245632 bytes)
INFO - Calling Whisper API...
INFO - Transcription completed in 1234 ms: Wie ist das Wetter...
INFO - LLM processing completed in 3456 ms
INFO - Voice command processed successfully in 4690 ms
```

## Nächste Schritte

### Optional: Text-to-Speech (TTS)
Für vollständigen Voice-to-Voice Agent:
1. LLM-Antwort durch TTS-API senden
2. Audio-Response zurückgeben
3. Neuer Endpoint: `/api/voice/process-with-tts`

### Optional: Streaming
Für Echtzeit-Feedback:
1. WebSocket-Verbindung
2. Streaming-Transkription
3. Streaming-LLM-Response

## Swagger Dokumentation

Vollständige API-Dokumentation verfügbar unter:
```
http://localhost:8084/swagger-ui.html
```

## Lizenz

Teil des AI Advent Challenge Projekts.
