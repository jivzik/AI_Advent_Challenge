package de.jivz.agentservice.service.parser;


import de.jivz.agentservice.dto.ToolResponse;

/**
 * Strategy Interface für verschiedene Response-Parser.
 * Ermöglicht flexible Parsing-Strategien für unterschiedliche Response-Formate.
 */
public interface ResponseParserStrategy {

    /**
     * Prüft ob diese Strategie für die gegebene Response anwendbar ist.
     *
     * @param response Die zu parsende Response
     * @return true wenn diese Strategie anwendbar ist
     */
    boolean canParse(String response);

    /**
     * Parst die Response zu einem ToolResponse-Objekt.
     *
     * @param response Die zu parsende Response
     * @return Das geparste ToolResponse-Objekt
     * @throws ResponseParsingException wenn das Parsing fehlschlägt
     */
    ToolResponse parse(String response) throws ResponseParsingException;
}

