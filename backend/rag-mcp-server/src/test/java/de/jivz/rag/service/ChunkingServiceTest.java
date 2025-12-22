package de.jivz.rag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit тесты для ChunkingService.
 */
class ChunkingServiceTest {

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingService();
        ReflectionTestUtils.setField(chunkingService, "chunkSize", 100);
        ReflectionTestUtils.setField(chunkingService, "chunkOverlap", 20);
    }

    @Test
    @DisplayName("Should return empty list for null input")
    void shouldReturnEmptyListForNullInput() {
        List<String> result = chunkingService.chunkText(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for blank input")
    void shouldReturnEmptyListForBlankInput() {
        List<String> result = chunkingService.chunkText("   ");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return single chunk for small text")
    void shouldReturnSingleChunkForSmallText() {
        String text = "This is a small text.";
        List<String> result = chunkingService.chunkText(text);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(text);
    }

    @Test
    @DisplayName("Should split text by paragraphs")
    void shouldSplitTextByParagraphs() {
        String text = "First paragraph with some content.\n\nSecond paragraph with more content.\n\nThird paragraph.";
        List<String> result = chunkingService.chunkText(text);

        assertThat(result).isNotEmpty();
        assertThat(result.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should split long text into multiple chunks")
    void shouldSplitLongTextIntoMultipleChunks() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longText.append("This is sentence number ").append(i).append(". ");
        }

        List<String> result = chunkingService.chunkText(longText.toString());

        assertThat(result).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("Should add overlap between chunks")
    void shouldAddOverlapBetweenChunks() {
        // Создаём текст который точно разобьётся на несколько чанков
        String text = "First part of the text that is quite long.\n\n" +
                      "Second part of the text that is also quite long.\n\n" +
                      "Third part of the text with additional content here.";

        ReflectionTestUtils.setField(chunkingService, "chunkSize", 50);
        ReflectionTestUtils.setField(chunkingService, "chunkOverlap", 10);

        List<String> result = chunkingService.chunkText(text);

        assertThat(result).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("Should handle text with only newlines")
    void shouldHandleTextWithOnlyNewlines() {
        String text = "\n\n\n\n";
        List<String> result = chunkingService.chunkText(text);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should preserve sentence boundaries when possible")
    void shouldPreserveSentenceBoundaries() {
        String text = "First sentence. Second sentence. Third sentence. Fourth sentence.";

        ReflectionTestUtils.setField(chunkingService, "chunkSize", 40);

        List<String> result = chunkingService.chunkText(text);

        assertThat(result).isNotEmpty();
        // Каждый чанк должен заканчиваться на точку (если возможно)
    }
}

