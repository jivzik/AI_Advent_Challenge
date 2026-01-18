package de.jivz.agentservice.cli.parser;

import de.jivz.agentservice.cli.domain.Command;
import de.jivz.agentservice.dto.Message;
import de.jivz.agentservice.service.client.OpenRouterApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses natural language commands using AI classification.
 * Single Responsibility: Command parsing and intent detection.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CommandParser {

    private final OpenRouterApiClient openRouterApiClient;

    private static final String SYSTEM_PROMPT = """
        You are a command parser for a DevOps CLI tool. 
        Parse the user's input and respond ONLY with one of these command types:
        
        DEPLOY - when user wants to deploy a service
        STATUS - when user wants to see service status
        LOGS - when user wants to view logs
        HEALTH - when user wants to check health
        RELEASE_NOTES - when user wants to generate release notes
        CREATE_RELEASE - when user wants to create a GitHub release
        COMMITS - when user wants to see commits
        ROLLBACK - when user wants to rollback a service
        COMMIT - when user wants to commit changes to git
        HELP - when user asks for help
        EXIT - when user wants to exit (quit, exit, bye)
        UNKNOWN - if you cannot determine the intent
        
        Also extract the service name if mentioned (team-service, support-service, rag-service, etc.)
        For COMMIT commands, extract the commit message.
        
        Respond ONLY in this format:
        TYPE: <command_type>
        SERVICE: <service_name or commit_message or NONE>
        LANGUAGE: <en or de>
        
        Examples:
        Input: "deploy team-service"
        TYPE: DEPLOY
        SERVICE: team-service
        LANGUAGE: en
        
        Input: "покажи статус"
        TYPE: STATUS
        SERVICE: NONE
        LANGUAGE: de
        
        Input: "commit with message add new feature"
        TYPE: COMMIT
        SERVICE: add new feature
        LANGUAGE: en
        
        Input: "закоммить изменения добавлен CLI"
        TYPE: COMMIT
        SERVICE: добавлен CLI
        LANGUAGE: de
        """;

    /**
     * Parse user input into a Command
     */
    public Command parse(String input) {
        if (input == null || input.isBlank()) {
            return Command.builder()
                .type(Command.CommandType.UNKNOWN)
                .rawInput(input)
                .build();
        }

        String trimmedInput = input.trim();

        // Try simple pattern matching first (faster)
        Command simpleCommand = trySimplePatternMatching(trimmedInput);
        if (simpleCommand != null) {
            log.debug("Command parsed via pattern matching: {}", simpleCommand.getType());
            return simpleCommand;
        }

        // Fall back to AI-based parsing for complex natural language
        log.debug("Using AI for command parsing: {}", trimmedInput);
        return parseWithAI(trimmedInput);
    }

    /**
     * Try to match simple commands without AI
     */
    private Command trySimplePatternMatching(String input) {
        String lower = input.toLowerCase();

        // Exit commands
        if (lower.matches("^(exit|quit|bye|выход)$")) {
            return Command.builder()
                .type(Command.CommandType.EXIT)
                .rawInput(input)
                .language(lower.contains("выход") ? "de" : "en")
                .build();
        }

        // Help commands
        if (lower.matches("^(help|помощь|\\?)$")) {
            return Command.builder()
                .type(Command.CommandType.HELP)
                .rawInput(input)
                .language(lower.contains("помощь") ? "de" : "en")
                .build();
        }

        // Status commands
        if (lower.matches("^(status|статус)$")) {
            return Command.builder()
                .type(Command.CommandType.STATUS)
                .rawInput(input)
                .language(lower.contains("статус") ? "de" : "en")
                .build();
        }

        // Deploy commands with service name
        Pattern deployPattern = Pattern.compile("^(deploy|задеплой)\\s+([a-z-]+)$");
        Matcher deployMatcher = deployPattern.matcher(lower);
        if (deployMatcher.matches()) {
            return Command.builder()
                .type(Command.CommandType.DEPLOY)
                .serviceName(deployMatcher.group(2))
                .rawInput(input)
                .language(deployMatcher.group(1).equals("задеплой") ? "de" : "en")
                .build();
        }

        // Logs commands
        Pattern logsPattern = Pattern.compile("^(logs?|логи?)\\s+([a-z-]+)$");
        Matcher logsMatcher = logsPattern.matcher(lower);
        if (logsMatcher.matches()) {
            return Command.builder()
                .type(Command.CommandType.LOGS)
                .serviceName(logsMatcher.group(2))
                .rawInput(input)
                .language(logsMatcher.group(1).startsWith("лог") ? "de" : "en")
                .build();
        }

        return null;
    }

    /**
     * Parse command using AI
     */
    private Command parseWithAI(String input) {
        try {
            List<Message> messages = List.of(
                Message.builder().role("system").content(SYSTEM_PROMPT).build(),
                Message.builder().role("user").content(input).build()
            );

            String response = openRouterApiClient.sendContextDetectionRequest(messages);

            return parseAIResponse(response, input);

        } catch (Exception e) {
            log.error("Failed to parse command with AI: {}", e.getMessage());
            return Command.builder()
                .type(Command.CommandType.UNKNOWN)
                .rawInput(input)
                .build();
        }
    }

    /**
     * Parse AI response into Command object
     */
    private Command parseAIResponse(String aiResponse, String originalInput) {
        try {
            Command.CommandType type = Command.CommandType.UNKNOWN;
            String serviceName = null;
            String language = "en";

            String[] lines = aiResponse.split("\n");
            for (String line : lines) {
                if (line.startsWith("TYPE:")) {
                    String typeStr = line.substring(5).trim();
                    try {
                        type = Command.CommandType.valueOf(typeStr);
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown command type from AI: {}", typeStr);
                    }
                } else if (line.startsWith("SERVICE:")) {
                    String service = line.substring(8).trim();
                    if (!"NONE".equals(service)) {
                        serviceName = service;
                    }
                } else if (line.startsWith("LANGUAGE:")) {
                    language = line.substring(9).trim();
                }
            }

            return Command.builder()
                .type(type)
                .serviceName(serviceName)
                .rawInput(originalInput)
                .language(language)
                .build();

        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            return Command.builder()
                .type(Command.CommandType.UNKNOWN)
                .rawInput(originalInput)
                .build();
        }
    }

    /**
     * Extract commit message from various formats:
     * commit "message"
     * commit message "text"
     * коммит "сообщение"
     */
    private String extractCommitMessage(String input) {
        // Try to find text in quotes (support both " and ")
        Pattern quotesPattern = Pattern.compile("[\"\\u201C]([^\"\\u201D]+)[\"\\u201D]");
        Matcher matcher = quotesPattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Try to extract after "message" keyword
        String lower = input.toLowerCase();
        int messageIdx = lower.indexOf("message");
        int messageIdxRu = lower.indexOf("сообщение");

        if (messageIdx > 0) {
            String afterMessage = input.substring(messageIdx + 7).trim();
            if (afterMessage.startsWith("\"")) {
                return afterMessage.substring(1, afterMessage.lastIndexOf("\""));
            }
            return afterMessage;
        }

        if (messageIdxRu > 0) {
            return input.substring(messageIdxRu + 9).trim();
        }

        // Extract everything after "commit" or "коммит"
        if (lower.startsWith("commit ")) {
            return input.substring(7).trim();
        } else if (lower.startsWith("коммит ") || lower.startsWith("закоммить ")) {
            int idx = lower.startsWith("коммит") ? 7 : 10;
            return input.substring(idx).trim();
        }

        return null;
    }
}

