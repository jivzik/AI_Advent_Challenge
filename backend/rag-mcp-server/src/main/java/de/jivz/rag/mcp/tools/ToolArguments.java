package de.jivz.rag.mcp.tools;

import java.util.Map;
import java.util.Optional;

/**
 * Типобезопасный хелпер для извлечения аргументов.
 */
public record ToolArguments(Map<String, Object> args) {

    public static ToolArguments of(Map<String, Object> args) {
        return new ToolArguments(args != null ? args : Map.of());
    }

    public String getRequiredString(String key) {
        return getString(key)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException(key + " is required"));
    }

    public Optional<String> getString(String key) {
        Object value = args.get(key);
        return value != null ? Optional.of(value.toString()) : Optional.empty();
    }

    public int getInt(String key, int defaultValue) {
        Object value = args.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        Object value = args.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Long getLong(String key, Long defaultValue) {
        Object value = args.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Long getRequiredLong(String key) {
        Long value = getLong(key, null);
        if (value == null) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }
}