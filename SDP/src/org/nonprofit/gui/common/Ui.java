package org.nonprofit.gui.common;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.Function;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Small building blocks so every screen looks the same: titles, toolbars,
 * sections, tables and the text shown in table cells. Views only.
 */
public final class Ui {

    private Ui() {
    }

    // ---- Layout ----

    /** Screen title with a one-line description under it. */
    public static VBox header(String title, String subtitle) {
        Label heading = new Label(title);
        heading.getStyleClass().add("screen-title");
        Label sub = new Label(subtitle);
        sub.getStyleClass().add("screen-subtitle");
        sub.setWrapText(true);
        return new VBox(2, heading, sub);
    }

    public static Button button(String text, String id) {
        Button button = new Button(text);
        button.setId(id);
        button.setMnemonicParsing(false);
        return button;
    }

    /** A row of buttons (and {@link #gap()}s) above a table. */
    public static HBox toolbar(Node... nodes) {
        HBox bar = new HBox(nodes);
        bar.getStyleClass().add("toolbar");
        return bar;
    }

    /** A thin vertical line between groups of buttons in a toolbar. */
    public static Separator gap() {
        Separator separator = new Separator(Orientation.VERTICAL);
        separator.setPadding(new Insets(0, 4, 0, 4));
        return separator;
    }

    /** Pushes the nodes after it to the right end of an HBox. */
    public static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /** A white card: title, toolbar, then the content (usually a table), which takes the spare height. */
    public static VBox section(String title, Node toolbar, Node content) {
        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        VBox box = new VBox(heading, toolbar, content);
        box.getStyleClass().add("section");
        VBox.setVgrow(content, Priority.ALWAYS);
        return box;
    }

    /** The line at the bottom of a screen that says what the last action did. */
    public static Label statusBar(String id) {
        Label status = new Label("Ready.");
        status.setId(id);
        status.getStyleClass().add("status-bar");
        status.setMaxWidth(Double.MAX_VALUE);
        return status;
    }

    /** The "Coming soon" screen shown for a module whose GUI is not built yet. */
    public static Node placeholder(String module) {
        Label title = new Label(module);
        title.getStyleClass().add("placeholder-title");
        Label text = new Label("Coming soon: this screen is being built. "
                + "Meanwhile every feature is in the console app (java -cp bin App).");
        text.getStyleClass().add("placeholder-text");
        text.setWrapText(true);
        text.setMaxWidth(480);
        VBox box = new VBox(10, title, text);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("screen");
        box.setId("placeholder-" + module.toLowerCase(Locale.ROOT));
        return box;
    }

    // ---- Tables ----

    /** An empty table that shares out its width between the columns. */
    public static <T> TableView<T> table(String id, String emptyText) {
        TableView<T> table = new TableView<>();
        table.setId(id);
        table.setPlaceholder(new Label(emptyText));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        return table;
    }

    /**
     * A read-only text column. The value is shown with {@link #display}, so it
     * may be a String, a number, an enum, a collection or null.
     */
    public static <T> TableColumn<T, String> column(String title, double prefWidth, Function<T, ?> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setPrefWidth(prefWidth);
        column.setMinWidth(Math.min(prefWidth, 50));
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(display(value.apply(cell.getValue()))));
        return column;
    }

    /** Replaces the rows and selects the same record again (matched by id) if it is still there. */
    public static <T> void show(TableView<T> table, List<T> rows, Function<T, String> idOf) {
        T selected = table.getSelectionModel().getSelectedItem();
        String selectedId = selected == null ? null : idOf.apply(selected);
        table.getItems().setAll(rows);
        if (selectedId != null) {
            select(table, idOf, selectedId);
        }
    }

    /** Selects the row whose id is {@code id}; false if there is none. */
    public static <T> boolean select(TableView<T> table, Function<T, String> idOf, String id) {
        for (T row : table.getItems()) {
            if (idOf.apply(row).equals(id)) {
                table.getSelectionModel().select(row);
                table.scrollTo(row);
                return true;
            }
        }
        return false;
    }

    // ---- Text shown in cells and choices ----

    /** null → "", enums → "Social media" (short ones stay "SMS"), collections → "a, b", numbers with a fraction → "6.0". */
    public static String display(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof DayOfWeek) {
            return ((DayOfWeek) value).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        }
        if (value instanceof Enum<?>) {
            String name = ((Enum<?>) value).name();
            if (name.equals("SMS") || name.equals("VIP")) {
                return name; // abbreviations stay in capitals
            }
            name = name.replace('_', ' ').toLowerCase(Locale.ROOT);
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        if (value instanceof Double || value instanceof Float) {
            return String.format(Locale.ROOT, "%.1f", ((Number) value).doubleValue());
        }
        if (value instanceof Collection<?>) {
            List<String> parts = new ArrayList<>();
            for (Object item : (Collection<?>) value) {
                parts.add(display(item));
            }
            return String.join(", ", parts);
        }
        return value.toString();
    }

    /** "Mon, Sat": short day names for a table cell, Monday first. */
    public static String days(Collection<DayOfWeek> days) {
        List<String> parts = new ArrayList<>();
        for (DayOfWeek day : new TreeSet<>(days)) {
            parts.add(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
        }
        return String.join(", ", parts);
    }
}
