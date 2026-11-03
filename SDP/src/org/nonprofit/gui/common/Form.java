package org.nonprofit.gui.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes a form (title and labelled fields) without any JavaFX nodes.
 * A controller builds one, {@link Dialogs#form} shows it, and the answers
 * come back as {@link FormValues} under each field's key.
 *
 * <pre>
 * Form form = new Form("Add a task")
 *         .text("name", "Task name", "")
 *         .choice("day", "Day", List.of(DayOfWeek.values()), DayOfWeek.SATURDAY)
 *         .date("date", "Date", LocalDate.now());
 * dialogs.form(form).ifPresent(values -&gt; ... values.text("name") ...);
 * </pre>
 */
public final class Form {

    public enum Kind { TEXT, CHOICE, CHECKS, CHECK, DATE }

    /**
     * One field. {@code options} is used by CHOICE and CHECKS; {@code initial}
     * is a String (TEXT), an option (CHOICE), a List of options (CHECKS),
     * a Boolean (CHECK) or a LocalDate (DATE).
     */
    public record Field(Kind kind, String key, String label, List<?> options, Object initial, String hint) {
    }

    private final String title;
    private String header;
    private final List<Field> fields = new ArrayList<>();

    public Form(String title) {
        this.title = title;
    }

    /** Optional line of explanation shown above the fields. */
    public Form header(String text) {
        this.header = text;
        return this;
    }

    public Form text(String key, String label, String initial) {
        return text(key, label, initial, null);
    }

    /** @param hint grey example text shown while the field is empty, e.g. "first aid, driving" */
    public Form text(String key, String label, String initial, String hint) {
        return add(new Field(Kind.TEXT, key, label, List.of(), initial == null ? "" : initial, hint));
    }

    /** A drop-down list; {@code initial} may be null (nothing chosen yet). Enum values are shown as "Social media". */
    public <T> Form choice(String key, String label, List<T> options, T initial) {
        return add(new Field(Kind.CHOICE, key, label, List.copyOf(options), initial, null));
    }

    /** A row of check boxes, one per option; the answer is the list of ticked options. */
    public <T> Form checks(String key, String label, List<T> options, Collection<T> selected) {
        return add(new Field(Kind.CHECKS, key, label, List.copyOf(options), List.copyOf(selected), null));
    }

    public Form check(String key, String label, boolean initial) {
        return add(new Field(Kind.CHECK, key, label, List.of(), initial, null));
    }

    public Form date(String key, String label, LocalDate initial) {
        return add(new Field(Kind.DATE, key, label, List.of(), initial, null));
    }

    private Form add(Field field) {
        for (Field existing : fields) {
            if (existing.key().equals(field.key())) {
                throw new IllegalArgumentException("Duplicate form key: " + field.key());
            }
        }
        fields.add(field);
        return this;
    }

    public String getTitle() { return title; }
    public String getHeader() { return header; }
    public List<Field> getFields() { return List.copyOf(fields); }

    /** The answers the user would give by pressing OK without changing anything (handy in tests). */
    public FormValues defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Field field : fields) {
            values.put(field.key(), field.initial());
        }
        return new FormValues(values);
    }
}
