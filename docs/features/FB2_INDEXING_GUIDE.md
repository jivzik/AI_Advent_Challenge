# FB2 Dateien Indexieren - Quickstart Guide

## Überblick

Das System unterstützt jetzt die Indexierung von **FB2 (FictionBook 2.0)** Dateien - einem weit verbreiteten eBook-Format, besonders in Russland und anderen Ländern mit kyrillischem Alphabet.

## Was ist FB2?

FB2 ist ein offenes XML-basiertes Format für digitale Bücher mit folgenden Merkmalen:
- **Format**: XML-basiert
- **Struktur**: Hierarchische Kapitel, Abschnitte und Paragraphen
- **Metadaten**: Vollständige Bibliotheksinformationen (Autor, Titel, Genre, etc.)
- **Encoding**: Normalerweise UTF-8
- **Zielgruppe**: Hauptsächlich russische und osteuropäische eBooks

## Installation

Die erforderlichen Abhängigkeiten wurden bereits hinzugefügt:

```xml
<!-- FB2 Parser Library -->
<dependency>
    <groupId>io.github.martin-ka</groupId>
    <artifactId>fb2-converter</artifactId>
    <version>1.2.0</version>
</dependency>

<!-- JAXB for XML Parsing -->
<dependency>
    <groupId>jakarta.xml.bind</groupId>
    <artifactId>jakarta.xml.bind-api</artifactId>
    <version>4.0.1</version>
</dependency>
<dependency>
    <groupId>org.glassfish.jaxb</groupId>
    <artifactId>jaxb-runtime</artifactId>
    <version>4.0.5</version>
</dependency>
```

## Verwendung

### 1. FB2-Datei hochladen

```bash
curl -X POST \
  -F "file=@example.fb2" \
  http://localhost:8080/api/documents/upload
```

Der Server erkennt automatisch das `.fb2` Format.

### 2. Text-Extraktion

Der `DocumentParserService` extrahiert automatisch Text aus FB2-Dateien:

```java
MultipartFile fb2File = ...;
String extractedText = documentParserService.extractText(fb2File);
```

### 3. Datei-Typ-Erkennung

```java
String fileType = documentParserService.getFileType("book.fb2");
// Ergebnis: "FB2"
```

## Technische Details

### FB2TextExtractor-Klasse

Die neue Klasse `FB2TextExtractor` ist ein SAX-basierter Handler, der:

1. **Text aus folgenden Elementen extrahiert**:
   - `<p>` - Paragraphen
   - `<title>` - Titel und Kapitelüberschriften
   - `<subtitle>` - Untertitel
   - `<section>` - Abschnitte
   - `<stanza>` - Gedichtstrophen
   - `<citation>` - Zitate
   - `<annotation>` - Anmerkungen
   - `<v>` - Verse
   - `<td>`, `<th>` - Tabellenzellen

2. **Ignoriert folgende Elemente**:
   - `<description>` - Metadaten und Beschreibungen
   - `<stylesheet>` - CSS-Stile
   - `<binary>` - Binärdaten (Bilder)
   - `<image>` - Bild-Verweise

### Parsing-Prozess

```
FB2-Datei
    ↓
[Versuche SAX-Parsing mit FB2TextExtractor]
    ├─ Erfolg → Extrahierter Text
    │
    └─ Fehler → Fallback zu Tika
                ├─ Erfolg → Extrahierter Text
                └─ Fehler → IOException
```

### Sicherheit

Das SAX-Parsing ist konfiguriert, um folgende XXE (XML External Entity) Attacken zu verhindern:

```java
factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
```

## Beispiel: Kompletter Workflow

```java
// 1. Datei hochladen
MultipartFile fb2File = request.getFile("file");

// 2. Text extrahieren
DocumentParserService parser = ...;
String text = parser.extractText(fb2File);

// 3. In Chunks aufteilen
ChunkingService chunking = ...;
List<String> chunks = chunking.splitIntoChunks(text);

// 4. Embeddings erzeugen und in DB speichern
EmbeddingService embedding = ...;
for (String chunk : chunks) {
    double[] vector = embedding.embed(chunk);
    // Speichere in document_chunks Tabelle
}

// 5. Full-Text-Search Vektor wird automatisch generiert
// (text_vector GENERATED ALWAYS AS tsvector)

// 6. Suche ist sofort verfügbar
// SELECT * FROM document_chunks 
// WHERE text_vector @@ plainto_tsquery('simple', 'query')
```

## Unterstützte Formate (Übersicht)

| Format | Parser | Beschreibung |
|--------|--------|-------------|
| PDF | Apache PDFBox | Universelles Dokumentformat |
| EPUB | Apache Tika | E-Book Standard |
| **FB2** | **FB2TextExtractor** | **FictionBook 2.0 (Neu!)** |
| DOCX | Apache Tika | Microsoft Word |
| DOC | Apache Tika | Ältere Word-Formate |
| TXT | Native | Reine Text-Dateien |
| MD | Native | Markdown-Dateien |
| Code (*.java, *.py, etc.) | Native | Quellcode |
| HTML/XML | Apache Tika | Web-Formate |

## Fehlerbehandlung

Wenn das FB2-Parsing fehlschlägt, gibt es mehrere Fallback-Optionen:

1. **SAX-Parser-Fehler** → Versuche Tika
2. **Tika-Fehler** → Werfe `IOException`
3. **Beide Fehler** → Detaillierte Fehlerlog mit vollständiger Stack-Trace

Beispiel-Log:
```
⚠️ FB2 SAX parsing failed: org.xml.sax.SAXParseException; lineNumber: 5; columnNumber: 10
Trying Tika fallback...
✅ Extracted 45000 characters from FB2 via Tika fallback
```

## Performance

- **Extraction**: < 1 Sekunde für typische Bücher (100-500 KB)
- **Speicher**: Streaming-basiert, keine kompletten Datei-Laden in Speicher
- **Indexing**: Automatisch nach Extraction mit bestehenden Systemen

## Debugging

Aktiviere Debug-Logging für FB2-Parsing:

```properties
logging.level.de.jivz.rag.service.FB2TextExtractor=DEBUG
logging.level.de.jivz.rag.service.DocumentParserService=DEBUG
```

Dies wird zeigen:
- Welche Elemente extrahiert werden
- Anzahl der Zeichen
- Anzahl der Linien
- Parsing-Fehler und Fallbacks

## Weitere Ressourcen

- [FB2 Format Spezifikation](https://fictionbook.org/)
- [Apache Tika Dokumentation](https://tika.apache.org/)
- [XML SAX Parsing Best Practices](https://owasp.org/www-community/attacks/XML_External_Entity_(XXE)_Processing)

