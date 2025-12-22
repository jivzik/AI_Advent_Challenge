package de.jivz.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для разбивки текста на чанки.
 *
 * Использует Recursive Character Splitting:
 * 1. Попытка разбить по параграфам (\n\n)
 * 2. Если чанк слишком большой → по строкам (\n)
 * 3. Если всё ещё большой → по предложениям (.)
 * 4. В крайнем случае → по пробелам или символам
 */
@Service
@Slf4j
public class ChunkingService {

    @Value("${rag.chunking.chunk-size:500}")
    private int chunkSize;

    @Value("${rag.chunking.chunk-overlap:100}")
    private int chunkOverlap;

    private static final List<String> DEFAULT_SEPARATORS = List.of(
            "\n\n",  // Параграфы
            "\n",    // Строки
            ". ",    // Предложения
            "! ",
            "? ",
            "; ",
            ", ",
            " ",     // Слова
            ""       // Символы (fallback)
    );

    /**
     * Разбивает текст на чанки с overlap.
     */
    public List<String> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        log.debug("📄 Chunking text of length: {} (chunkSize={}, overlap={})",
                text.length(), chunkSize, chunkOverlap);

        List<String> chunks = recursiveSplit(text, DEFAULT_SEPARATORS);

        // Добавляем overlap между чанками
        List<String> chunksWithOverlap = addOverlap(chunks);

        log.info("✅ Created {} chunks from text", chunksWithOverlap.size());
        return chunksWithOverlap;
    }

    /**
     * Рекурсивная разбивка текста по сепараторам.
     */
    private List<String> recursiveSplit(String text, List<String> separators) {
        List<String> finalChunks = new ArrayList<>();

        if (text.length() <= chunkSize) {
            if (!text.isBlank()) {
                finalChunks.add(text.trim());
            }
            return finalChunks;
        }

        // Пробуем каждый сепаратор по очереди
        for (int i = 0; i < separators.size(); i++) {
            String separator = separators.get(i);

            if (separator.isEmpty()) {
                // Fallback: разбиваем по символам
                for (int j = 0; j < text.length(); j += chunkSize) {
                    int end = Math.min(j + chunkSize, text.length());
                    String chunk = text.substring(j, end).trim();
                    if (!chunk.isEmpty()) {
                        finalChunks.add(chunk);
                    }
                }
                return finalChunks;
            }

            if (!text.contains(separator)) {
                continue;
            }

            String[] parts = text.split(separator.equals(".") ? "\\." :
                                        java.util.regex.Pattern.quote(separator));

            StringBuilder currentChunk = new StringBuilder();
            for (String part : parts) {
                String trimmedPart = part.trim();
                if (trimmedPart.isEmpty()) continue;

                // Добавляем сепаратор обратно (кроме пробела)
                String partWithSep = separator.equals(" ") ? trimmedPart : trimmedPart + separator;

                if (currentChunk.length() + partWithSep.length() <= chunkSize) {
                    currentChunk.append(partWithSep);
                } else {
                    // Сохраняем текущий чанк
                    if (currentChunk.length() > 0) {
                        String chunk = currentChunk.toString().trim();
                        if (!chunk.isEmpty()) {
                            finalChunks.add(chunk);
                        }
                    }

                    // Если часть сама по себе больше chunkSize, рекурсивно разбиваем
                    if (partWithSep.length() > chunkSize) {
                        List<String> subSeparators = separators.subList(i + 1, separators.size());
                        finalChunks.addAll(recursiveSplit(partWithSep, subSeparators));
                        currentChunk = new StringBuilder();
                    } else {
                        currentChunk = new StringBuilder(partWithSep);
                    }
                }
            }

            // Добавляем последний чанк
            if (currentChunk.length() > 0) {
                String chunk = currentChunk.toString().trim();
                if (!chunk.isEmpty()) {
                    finalChunks.add(chunk);
                }
            }

            if (!finalChunks.isEmpty()) {
                return finalChunks;
            }
        }

        return finalChunks;
    }

    /**
     * Добавляет overlap между чанками для сохранения контекста.
     */
    private List<String> addOverlap(List<String> chunks) {
        if (chunks.size() <= 1 || chunkOverlap <= 0) {
            return chunks;
        }

        List<String> result = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            StringBuilder chunkWithOverlap = new StringBuilder();

            // Добавляем конец предыдущего чанка (overlap)
            if (i > 0) {
                String prevChunk = chunks.get(i - 1);
                int overlapStart = Math.max(0, prevChunk.length() - chunkOverlap);
                String overlap = prevChunk.substring(overlapStart);
                chunkWithOverlap.append(overlap);
                if (!overlap.endsWith(" ")) {
                    chunkWithOverlap.append(" ");
                }
            }

            chunkWithOverlap.append(chunks.get(i));
            result.add(chunkWithOverlap.toString().trim());
        }

        return result;
    }

    /**
     * Получить текущие настройки.
     */
    public int getChunkSize() {
        return chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }
}

