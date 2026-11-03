package org.nonprofit.gui.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The answers of a submitted {@link Form}, by field key. */
public final class FormValues {
    private final Map<String, Object> values;

    public FormValues(Map<String, ?> values) {
        // LinkedHashMap allows null values (an empty choice or date).
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /** A TEXT field, trimmed; "" when empty. */
    public String text(String key) {
        Object value = get(key);
        return value == null ? "" : value.toString().trim();
    }

    /** A CHOICE field; null when nothing was chosen. */
    public <T> T choice(String key, Class<T> type) {
        return type.cast(get(key));
    }

    /** A CHECKS field: the ticked options, in the form's order. */
    public <T> List<T> checked(String key, Class<T> type) {
        List<T> result = new ArrayList<>();
        Object value = get(key);
        if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                result.add(type.cast(item));
            }
        }
        return result;
    }

    /** A CHECK field. */
    public boolean check(String key) {
        return Boolean.TRUE.equals(get(key));
    }

    /** A DATE field; null when empty. */
    public LocalDate date(String key) {
        return (LocalDate) get(key);
    }

    /** A copy with one answer changed, e.g. {@code form.defaults().with("hours", "4")} in a test. */
    public FormValues with(String key, Object value) {
        get(key);
        Map<String, Object> copy = new LinkedHashMap<>(values);
        copy.put(key, value);
        return new FormValues(copy);
    }

    private Object get(String key) {
        if (!values.containsKey(key)) {
            throw new IllegalArgumentException("The form has no field " + key);
        }
        return values.get(key);
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
