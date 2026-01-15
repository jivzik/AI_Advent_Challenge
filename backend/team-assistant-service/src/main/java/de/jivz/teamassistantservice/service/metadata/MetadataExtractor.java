package de.jivz.teamassistantservice.service.metadata;

import java.util.List;

/**
 * Strategy interface for extracting metadata from responses.
 * Follows Open/Closed Principle - new extractors can be added without modification.
 */
public interface MetadataExtractor<T> {

    /**
     * Extracts metadata from the given input.
     *
     * @param input The input to extract metadata from
     * @return List of extracted metadata items
     */
    List<T> extract(String input);

    /**
     * Returns the type of metadata this extractor handles.
     *
     * @return Metadata type name
     */
    String getMetadataType();
}

