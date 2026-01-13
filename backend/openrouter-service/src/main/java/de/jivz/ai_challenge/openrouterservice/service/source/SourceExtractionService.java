package de.jivz.ai_challenge.openrouterservice.service.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service für Extraktion und Formatierung von Dokumenten-Quellen.
 * Spezialisiert auf RAG-Tool-Ergebnisse.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SourceExtractionService {

    private final ObjectMapper objectMapper;

    /**
     * Extrahiert Dokumentennamen aus einem RAG-Tool-Ergebnis.
     *
     * @param toolResult Das JSON-Ergebnis des RAG-Tools
     * @param sources Set zum Sammeln der Quellen
     */
    public void extractSourcesFromRagResult(String toolResult, Set<String> sources) {
        try {
            var results = objectMapper.readTree(toolResult);

            if (results.isArray()) {
                for (var result : results) {
                    String docName = result.path("documentName").asText();
                    if (isValidDocumentName(docName)) {
                        sources.add(docName);
                        log.debug("📚 Found document source: {}", docName);
                    }
                }
            }

            log.info("📚 Extracted {} unique sources from result", sources.size());

        } catch (Exception e) {
            log.warn("Could not extract sources from RAG result: {}", e.getMessage());
        }
    }

    /**
     * Fügt formatierte Quellen-Liste an die Antwort an.
     *
     * @param answer Die Antwort
     * @param sources Die Quellen
     * @return Die Antwort mit angehängten Quellen
     */
    public String appendSources(String answer, Set<String> sources) {
        if (sources.isEmpty()) {
            return answer;
        }

        StringBuilder sourcesSection = new StringBuilder();
        sourcesSection.append("\n\n---\n\n");
        sourcesSection.append("**📚 Quellen der Information:**\n");

        int index = 1;
        for (String source : sources) {
            sourcesSection.append(String.format("%d. `%s`\n", index++, source));
        }

        log.info("📚 Appended {} sources to answer", sources.size());
        return answer + sourcesSection.toString();
    }

    /**
     * Prüft ob ein Dokumentenname valide ist.
     *
     * @param docName Der Dokumentenname
     * @return true wenn valide
     */
    private boolean isValidDocumentName(String docName) {
        return docName != null
                && !docName.isBlank()
                && !docName.equals("null");
    }
}

