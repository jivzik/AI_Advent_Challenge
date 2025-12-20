package de.jivz.ai_challenge.service.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.mcp.model.ToolDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JSON-Schema Prompting für Reminder-Zusammenfassungen
 *
 * WICHTIG: Sonar und OpenRouter MÜSSEN die exakt gleiche JSON-Response-Struktur liefern!
 *
 * Diese Klasse enthält die vereinheitlichten Prompts für beide Provider,
 * damit sie DIREKT das richtige JSON-Format ausgeben.
 *
 * Kein Parsing, Normalisierung oder Konvertierung mehr nötig!
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReminderJsonPromptStrategy {

    private final ObjectMapper objectMapper;

    /**
     * Einheitliche JSON-Response-Struktur für BEIDE Provider (Sonar & OpenRouter)
     *
     * Diese Struktur wird zentral definiert und beide Provider müssen
     * exakt dieses Format liefern - keine Abweichungen!
     */
    private static final String UNIFIED_JSON_SCHEMA = """
        {
          "title": "Aufgaben-Zusammenfassung vom [DATUM]",
          "summary": "Kurze Zusammenfassung aller Aufgaben",
          "total_items": 5,
          "priority": "HIGH|MEDIUM|LOW",
          "highlights": [
            "Wichtiger Punkt 1",
            "Wichtiger Punkt 2"
          ],
          "active_tasks": [
            {
              "name": "Task-Name",
              "due_date": "2025-12-18 15:00",
              "description": "Beschreibung",
              "category": "SPORTS|WORK|HEALTH|FAMILY|OTHER",
              "urgency": "HIGH|MEDIUM|LOW"
            }
          ],
          "due_soon": [
            {
              "name": "Task-Name",
              "due_date": "2025-12-18 20:00",
              "description": "Beschreibung",
              "category": "SPORTS|WORK|HEALTH|FAMILY|OTHER",
              "urgency": "HIGH|MEDIUM|LOW"
            }
          ],
          "overdue": [
            {
              "name": "Task-Name",
              "due_date": "2025-12-15 14:00",
              "description": "3 Tage überfällig",
              "category": "SPORTS|WORK|HEALTH|FAMILY|OTHER",
              "urgency": "HIGH|MEDIUM|LOW"
            }
          ]
        }
        """;

    /**
     * Erstellt den System-Prompt mit dynamisch eingebetteten MCP Tools.
     *
     * @param tools Список доступных MCP Tools
     * @return Полный системный промпт
     */
    public String buildDynamicSystemPrompt(List<ToolDefinition> tools) {
        String toolsSection = formatToolsForPrompt(tools);
        return buildSystemPromptTemplate().formatted(toolsSection);
    }

    /**
     * Formatiert MCP Tools für Perplexity-Prompt-Integration.
     * Dies ist die Gegenstück zu convertToOpenRouterTools() in OpenRouterToolsPromptStrategy.
     *
     * @param tools Liste der MCP Tools die formatiert werden sollen
     * @return Formatierte Tools-Beschreibung für den Prompt
     */
    public String formatToolsForPrompt(List<ToolDefinition> tools) {
        StringBuilder toolsDescription = new StringBuilder();

        if (tools != null && !tools.isEmpty()) {
            toolsDescription.append("## Доступные MCP Tools:\n\n");

            for (int i = 0; i < tools.size(); i++) {
                ToolDefinition tool = tools.get(i);
                toolsDescription.append(String.format("%d. **%s**\n", i + 1, tool.getName()));
                toolsDescription.append(String.format("   - Описание: %s\n",
                        tool.getDescription() != null ? tool.getDescription() : "Описание недоступно"));

                if (tool.getInputSchema() != null && !tool.getInputSchema().isEmpty()) {
                    try {
                        String schemaJson = objectMapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(tool.getInputSchema());
                        toolsDescription.append(String.format("   - Схема: ```json\n%s\n```\n", schemaJson));
                    } catch (JsonProcessingException e) {
                        toolsDescription.append(String.format("   - Схема: %s\n", tool.getInputSchema()));
                    }
                }
                toolsDescription.append("\n");
            }
        } else {
            toolsDescription.append("## MCP Tools nicht verfügbar\n\n");
            toolsDescription.append("Derzeit sind keine externen Tools registriert.\n\n");
        }

        log.debug("Formatted {} MCP tools for Perplexity prompt", tools != null ? tools.size() : 0);
        return toolsDescription.toString();
    }



    /**
     * System-Prompt mit JSON-Schema für Sonar (Perplexity) and OpenRouter
     *
     * WICHTIG: Dieser Prompt beschreibt BEIDE Phasen:
     * 1. Phase 1: Wenn Tools verfügbar → Tool-Calls verwenden
     * 2. Phase 2: Nach Tool-Execution → Finales JSON
     */
    public static String buildSystemPromptTemplate() {
        return """
            Du bist ein intelligenter Assistent für Aufgaben-Verwaltung.
            
            %s
            
            WORKFLOW - FOLGE GENAU DIESEM PROZESS:
            
            ═══════════════════════════════════════════════════════════════════
            PHASE 1: TOOL-CALLS (wenn Tools verfügbar sind)
            ═══════════════════════════════════════════════════════════════════
            
            WENN dir Tools zur Verfügung stehen, MUSST du diese VERWENDEN!
            
            Antworte EXAKT mit diesem JSON-Format:
            {
              "step": "tool",
              "toolCalls": [
                {
                  "name": "tool_name",
                  "arguments": {...}
                }
              ],
              "answer": null
            }
            
            REGELN für Tool-Phase:
            - Rufe ALLE verfügbaren Tools auf
            - "step" MUSS genau "tool" sein
            - "toolCalls" ist ein Array von Tool-Aufrufen
            - "answer" ist NULL in dieser Phase
            - Keine Kommentare, NUR valides JSON!
            
            ═══════════════════════════════════════════════════════════════════
            PHASE 2: FINALES JSON (nach Tool-Results oder ohne Tools)
            ═══════════════════════════════════════════════════════════════════
            
            Nach der Verarbeitung von Tool-Ergebnissen, erstelle eine 
            strukturierte Zusammenfassung im folgenden Format:
            
            """ + UNIFIED_JSON_SCHEMA + """
            
            REGELN für Final-Phase:
            1. "step" MUSS "final" sein (oder fehlen, wenn es direktes JSON ist)
            2. Antworte NUR mit dem JSON-Objekt - nichts anderes!
            3. KEINE ```json Blöcke - reines JSON!
            4. Alle Felder müssen present sein (auch wenn leer: [] oder "")
            5. Priority = höchste Priorität aller Tasks (HIGH > MEDIUM > LOW)
            6. Kategorien: SPORTS, WORK, HEALTH, FAMILY, OTHER
            7. Urgency: HIGH (sofort), MEDIUM (diese Woche), LOW (später)
            8. due_date: ISO-Format (YYYY-MM-DD HH:mm) oder lesbar
            9. Gültiges JSON - keine Fehler!
            
            ═══════════════════════════════════════════════════════════════════
            WICHTIG - NICHT IGNORIEREN:
            ═══════════════════════════════════════════════════════════════════
            
            🔴 REGEL 1: Tool-Calls IMMER zuerst!
            Wenn Tools vorhanden sind, rufe sie ZUERST auf!
            Warte nicht, antworte nicht, rufe die Tools auf!
            
            🔴 REGEL 2: Keine Halluzinationen!
            Erfinde KEINE Daten! Verwende NUR Tool-Ergebnisse!
            
            🔴 REGEL 3: Nur JSON in den Antworten!
            Keine Markdown, keine Erklärungen, keine Floskeln!
            
            🔴 REGEL 4: Valides JSON!
            Überprüfe deine JSON-Syntax!
            Alle Strings haben Quotes!
            Alle Kommas sind correct!
            """;
    }

    /**
     * User-Prompt für beide Provider (Sonar & OpenRouter) mit Tool-Integration
     * IDENTISCH für beide - einheitlicher Workflow!
     *
     * Dieser Prompt wird NACH dem System-Prompt gesendet
     */
    public static String getJsonUserPrompt() {
        return """
            TASK: Erstelle eine detaillierte Zusammenfassung meiner Aufgaben.
            
            PROZESS:
            1. PHASE 1: Rufe die verfügbaren Tools auf, um meine Aufgaben zu laden
            2. PHASE 2: Analysiere die Tool-Ergebnisse
            3. PHASE 3: Erstelle die strukturierte Zusammenfassung als JSON
            
            CRITICAL: 
            ⚠️  Antworte ZUERST mit den Tool-Calls (Phase 1)!
            ⚠️  DANN mit dem finalen JSON (Phase 2)!
            ⚠️  Keine Erfindungen - nur echte Daten aus den Tools!
            """;
    }


    /**
     * JSON Schema für direkten Parsing
     * Kann auch für OpenRouter's JSON-Mode oder Structured Output verwendet werden
     */
    public static String getJsonSchema() {
        return """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "required": ["title", "summary", "total_items", "priority", "highlights", "active_tasks", "due_soon", "overdue"],
              "properties": {
                "title": {
                  "type": "string",
                  "description": "Titel der Zusammenfassung"
                },
                "summary": {
                  "type": "string",
                  "description": "Kurze Zusammenfassung"
                },
                "total_items": {
                  "type": "integer",
                  "description": "Gesamtanzahl Aufgaben"
                },
                "priority": {
                  "type": "string",
                  "enum": ["HIGH", "MEDIUM", "LOW"],
                  "description": "Gesamtpriorität"
                },
                "highlights": {
                  "type": "array",
                  "items": {"type": "string"},
                  "description": "Wichtige Punkte"
                },
                "active_tasks": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/task"
                  }
                },
                "due_soon": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/task"
                  }
                },
                "overdue": {
                  "type": "array",
                  "items": {
                    "$ref": "#/definitions/task"
                  }
                }
              },
              "definitions": {
                "task": {
                  "type": "object",
                  "required": ["name", "urgency"],
                  "properties": {
                    "name": {
                      "type": "string"
                    },
                    "due_date": {
                      "type": "string"
                    },
                    "description": {
                      "type": "string"
                    },
                    "category": {
                      "type": "string",
                      "enum": ["SPORTS", "WORK", "HEALTH", "FAMILY", "OTHER"]
                    },
                    "urgency": {
                      "type": "string",
                      "enum": ["HIGH", "MEDIUM", "LOW"]
                    }
                  }
                }
              }
            }
            """;
    }
}

