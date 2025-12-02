package de.jivz.ai_challenge.service.strategy;

import org.springframework.stereotype.Component;

/**
 * Strategy for auto-generated JSON schema instructions.
 * AI decides the best JSON structure based on the question.
 */
@Component
public class AutoSchemaInstructionStrategy implements JsonInstructionStrategy {

    @Override
    public boolean canHandle(String customSchema, boolean autoSchema) {
        return autoSchema;
    }

    @Override
    public String buildInstruction() {
        return """
                CRITICAL INSTRUCTION: Analyze the question and respond with valid JSON in the MOST APPROPRIATE format.
                
                INTELLIGENT SCHEMA SELECTION RULES:
                
                1. For SIMPLE questions (who, what, when, where, why):
                   Use: {"response": "your answer"}
                
                2. For LIST/COMPARISON requests (top N, compare, list):
                   Use: {"items": [{"name": "...", "description": "..."}, ...]}
                   OR: {"results": [{"title": "...", "details": "..."}, ...]}
                
                3. For STRUCTURED DATA (books by genre, products by category):
                   Create nested structure: {"categories": [{"name": "...", "items": [...]}, ...]}
                
                4. For MULTI-PART questions (explain X and list Y):
                   Use: {"explanation": "...", "list": [...]}
                
                5. For TABULAR DATA (specifications, comparisons):
                   Use: {"data": [{"column1": "...", "column2": "..."}, ...]}
                
                EXAMPLES:
                
                Q: "Who is the best singer?"
                A: {"response": "Taylor Swift is widely considered one of the best..."}
                
                Q: "Compare Java, Python, JavaScript"
                A: {"languages": [{"name": "Java", "type": "compiled", "strengths": "...", "weaknesses": "..."}, {"name": "Python", ...}]}
                
                Q: "List top 2 books in 3 genres"
                A: {"genres": [{"name": "Fantasy", "books": ["Book 1", "Book 2"]}, {"name": "Sci-Fi", "books": [...]}]}
                
                Q: "What is Spring Boot and list its features"
                A: {"definition": "Spring Boot is...", "features": ["Auto-configuration", "Embedded servers", ...]}
                
                STRICT RULES:
                - NO markdown formatting
                - NO code blocks (no ``` or ```json)
                - Just pure, valid JSON
                - Choose the structure that best fits the question
                - Be consistent in naming (use camelCase or snake_case throughout)
                """;
    }
}

