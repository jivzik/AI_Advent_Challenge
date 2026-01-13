package de.jivz.mcp.tools;

import java.util.Map;
import java.util.Optional;

/**
 * Typsicherer Helper für die Extraktion von Tool-Argumenten.
 *
 * Vereinfacht den Zugriff auf Arguments-Map mit Typ-Konvertierung
 * und Default-Werten.
 */
public record ToolArguments(Map<String, Object> args) {

    public static ToolArguments of(Map<String, Object> args) {
        return new ToolArguments(args != null ? args : Map.of());
    }

    /**
     * Pflicht-String-Parameter holen.
     */
    public String getRequiredString(String key) {
        return getString(key)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException(key + " ist erforderlich"));
    }

    /**
     * Optionalen String-Parameter holen.
     */
    public Optional<String> getString(String key) {
        Object value = args.get(key);
        return value != null ? Optional.of(value.toString()) : Optional.empty();
    }

    /**
     * String mit Default-Wert holen.
     */
    public String getString(String key, String defaultValue) {
        return getString(key).orElse(defaultValue);
    }

    /**
     * Integer mit Default-Wert holen.
     */
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

    /**
     * Double mit Default-Wert holen.
     */
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

    /**
     * Long mit Default-Wert holen.
     */
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

    /**
     * Boolean mit Default-Wert holen.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = args.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Prüfen, ob Argument vorhanden ist.
     */
    public boolean has(String key) {
        return args.containsKey(key);
    }

    /**
     * Liste mit Default-Wert holen.
     */
    @SuppressWarnings("unchecked")
    public <T> java.util.List<T> getList(String key, java.util.List<T> defaultValue) {
        Object value = args.get(key);
        if (value == null) return defaultValue;
        if (value instanceof java.util.List) {
            return (java.util.List<T>) value;
        }
        return defaultValue;
    }

    /**
     * Raw-Wert holen.
     */
    public Object get(String key) {
        return args.get(key);
    }
}

