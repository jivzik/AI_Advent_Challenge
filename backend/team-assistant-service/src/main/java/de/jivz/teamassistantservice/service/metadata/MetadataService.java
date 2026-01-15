package de.jivz.teamassistantservice.service.metadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Unified service for managing metadata extraction.
 * Coordinates different extractors following Single Responsibility Principle.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MetadataService {

    private final LlmSourceExtractor llmSourceExtractor;
    private final LlmToolsUsedExtractor llmToolsUsedExtractor;
    private final RagToolSourceExtractor ragToolSourceExtractor;

    /**
     * Extracts sources from LLM response.
     */
    public List<String> extractSourcesFromLlmResponse(String llmResponse) {
        return llmSourceExtractor.extract(llmResponse);
    }

    /**
     * Extracts tools used from LLM response.
     */
    public List<String> extractToolsUsedFromLlmResponse(String llmResponse) {
        return llmToolsUsedExtractor.extract(llmResponse);
    }

    /**
     * Extracts sources from RAG tool result.
     */
    public List<String> extractSourcesFromRagResult(String ragToolResult) {
        return ragToolSourceExtractor.extract(ragToolResult);
    }

    /**
     * Formats sources as a Russian-language section to append to answers.
     *
     * @param answer The answer text
     * @param sources The sources to append
     * @return Answer with appended sources
     */
    public String appendSourcesToAnswer(String answer, Set<String> sources) {
        if (sources == null || sources.isEmpty()) {
            return answer;
        }

        StringBuilder sourcesSection = new StringBuilder();
        sourcesSection.append("\n\n---\n\n");
        sourcesSection.append("**📚 Источники:**\n");

        int index = 1;
        for (String source : sources) {
            sourcesSection.append(String.format("%d. `%s`\n", index++, source));
        }

        log.info("📚 Appended {} sources to answer", sources.size());
        return answer + sourcesSection.toString();
    }

    /**
     * Merges sources from RAG tool results into the sources set.
     */
    public void mergeRagSources(String ragToolResult, Set<String> sources) {
        List<String> extractedSources = extractSourcesFromRagResult(ragToolResult);
        sources.addAll(extractedSources);
    }

    /**
     * Creates a unified set of sources from both LLM and RAG.
     */
    public Set<String> mergeAllSources(String llmResponse, Set<String> ragSources) {
        Set<String> allSources = new LinkedHashSet<>();

        // Add sources from LLM response
        List<String> llmSources = extractSourcesFromLlmResponse(llmResponse);
        allSources.addAll(llmSources);

        // Add RAG sources
        if (ragSources != null) {
            allSources.addAll(ragSources);
        }

        log.debug("📚 Merged sources: {} from LLM + {} from RAG = {} total",
                llmSources.size(), ragSources != null ? ragSources.size() : 0, allSources.size());

        return allSources;
    }
}

