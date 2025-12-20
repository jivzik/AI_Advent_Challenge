# MCP Service - Google Tasks Integration

Ein Spring Boot Service, der Model Context Protocol (MCP) Tool-Definitionen für die Google Tasks API bereitstellt.

## Übersicht

Der `GoogleServiceToolDefinition` Service definiert eine umfassende Schnittstelle für die Verwaltung von Google Tasks durch standardisierte Tool-Definitionen.

## Verfügbare Tools

### 1. **google_tasks_list**
Ruft alle Google Tasks Listen des authentifizierten Benutzers ab.
- **Parameter:** Keine
- **Verwendungsfall:** Alle verfügbaren Aufgabenlisten auflisten

### 2. **google_tasks_get**
Ruft alle Aufgaben aus einer bestimmten Google Tasks Liste ab.
- **Parameter:**
  - `taskListId` (string, optional): ID der Aufgabenliste (verwendet Standard, falls nicht angegeben)

### 3. **google_tasks_create**
Erstellt eine neue Aufgabe in einer Google Tasks Liste.
- **Parameter:**
  - `taskListId` (string, optional): ID der Aufgabenliste
  - `title` (string, erforderlich): Titel der Aufgabe
  - `notes` (string, optional): Notizen oder Beschreibung
  - `due` (string, optional): Fälligkeitsdatum im ISO 8601 Format (z.B. 2024\-12\-31T23:59:59Z)

### 4. **google_tasks_update**
Aktualisiert eine existierende Aufgabe.
- **Parameter:**
  - `taskListId` (string, optional): ID der Aufgabenliste
  - `taskId` (string, erforderlich): ID der zu aktualisierenden Aufgabe
  - `title` (string, optional): Neuer Titel
  - `notes` (string, optional): Neue Notizen
  - `status` (string, optional): Status (`needsAction` oder `completed`)

### 5. **google_tasks_complete**
Kennzeichnet eine Aufgabe als abgeschlossen.
- **Parameter:**
  - `taskListId` (string, optional): ID der Aufgabenliste
  - `taskId` (string, erforderlich): ID der Aufgabe

### 6. **google_tasks_delete**
Löscht eine Aufgabe aus Google Tasks.
- **Parameter:**
  - `taskListId` (string, optional): ID der Aufgabenliste
  - `taskId` (string, erforderlich): ID der zu löschenden Aufgabe

## Architektur

- **Framework:** Spring Boot
- **Sprache:** Java
- **Build Tool:** Maven
- **Pattern:** Service-Klasse mit Builder-Pattern für Tool-Definition

## Verwendung

Die Tool-Definitionen können über die `getToolDefinitions()`\-Methode abgerufen werden, die eine Liste von `McpTool`\-Objekten zurückgibt.