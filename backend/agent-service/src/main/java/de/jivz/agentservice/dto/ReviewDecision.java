package de.jivz.agentservice.dto;

/**
 * Решение code review агента
 */
public enum ReviewDecision {
    /**
     * ✅ APPROVE - код готов к merge
     * - Нет critical/major issues
     * - Код соответствует стандартам
     * - Minor issues допустимы
     */
    APPROVE,

    /**
     * ❌ REQUEST_CHANGES - требуются изменения перед merge
     * - Есть critical issues (security, bugs, breaking changes)
     * - Есть 5+ major issues
     * - Отсутствуют критически важные тесты
     */
    REQUEST_CHANGES,

    /**
     * 💬 COMMENT - есть замечания, но не блокирующие
     * - Только minor issues
     * - Предложения по улучшению
     * - Вопросы для уточнения
     */
    COMMENT;

    /**
     * Проверяет требуются ли изменения
     */
    public boolean requiresChanges() {
        return this == REQUEST_CHANGES;
    }

    /**
     * Проверяет одобрен ли PR
     */
    public boolean isApproved() {
        return this == APPROVE;
    }

    /**
     * Получает emoji для UI
     */
    public String getEmoji() {
        switch (this) {
            case APPROVE: return "✅";
            case REQUEST_CHANGES: return "❌";
            case COMMENT: return "💬";
            default: return "❓";
        }
    }

    /**
     * Получает GitHub review state
     * https://docs.github.com/en/rest/pulls/reviews#create-a-review-for-a-pull-request
     */
    public String toGitHubState() {
        switch (this) {
            case APPROVE: return "APPROVE";
            case REQUEST_CHANGES: return "REQUEST_CHANGES";
            case COMMENT: return "COMMENT";
            default: return "COMMENT";
        }
    }
}