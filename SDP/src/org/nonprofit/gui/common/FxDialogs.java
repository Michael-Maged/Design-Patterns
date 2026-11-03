package org.nonprofit.gui.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

/** The real {@link Dialogs}: JavaFX alerts and forms that wait for the user. */
public final class FxDialogs implements Dialogs {
    private final Window owner;

    /** @param owner the window the dialogs belong to (may be null) */
    public FxDialogs(Window owner) {
        this.owner = owner;
    }

    @Override
    public void error(String message) {
        alert(Alert.AlertType.ERROR, "Could not do that", message);
    }

    @Override
    public void info(String message) {
        alert(Alert.AlertType.INFORMATION, "Information", message);
    }

    @Override
    public boolean confirm(String question) {
        Alert alert = alert(Alert.AlertType.CONFIRMATION, "Please confirm", question, false);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    @Override
    public void text(String title, String text) {
        Dialog<Void> dialog = new Dialog<>();
        prepare(dialog, title);
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(false);
        area.getStyleClass().add("mono");
        area.setPrefSize(660, 420);
        dialog.getDialogPane().setContent(area);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Close", ButtonBar.ButtonData.OK_DONE));
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    @Override
    public Optional<FormValues> form(Form form) {
        Dialog<ButtonType> dialog = new Dialog<>();
        prepare(dialog, form.getTitle());
        dialog.setHeaderText(form.getHeader());

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.setHgap(12);
        grid.setVgap(10);
        Map<String, Supplier<Object>> readers = new LinkedHashMap<>();
        Node first = null;
        int row = 0;
        for (Form.Field field : form.getFields()) {
            Node control = control(field, readers);
            if (first == null) {
                first = control;
            }
            Label label = new Label(field.label());
            label.getStyleClass().add("form-label");
            grid.addRow(row++, label, control);
            GridPane.setHgrow(control, Priority.ALWAYS);
        }
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (first != null) {
            Node focus = first;
            Platform.runLater(focus::requestFocus);
        }

        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return Optional.empty();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        readers.forEach((key, reader) -> values.put(key, reader.get()));
        return Optional.of(new FormValues(values));
    }

    // ---- Building the controls ----

    private static Node control(Form.Field field, Map<String, Supplier<Object>> readers) {
        switch (field.kind()) {
            case TEXT: {
                TextField text = new TextField((String) field.initial());
                text.setPrefColumnCount(26);
                if (field.hint() != null) {
                    text.setPromptText(field.hint());
                }
                readers.put(field.key(), text::getText);
                return text;
            }
            case CHOICE: {
                ComboBox<Object> combo = new ComboBox<>();
                combo.getItems().addAll(field.options());
                combo.setValue(field.initial());
                combo.setConverter(new StringConverter<Object>() {
                    @Override
                    public String toString(Object value) {
                        return Ui.display(value);
                    }

                    @Override
                    public Object fromString(String text) {
                        return null;
                    }
                });
                combo.setMaxWidth(Double.MAX_VALUE);
                readers.put(field.key(), combo::getValue);
                return combo;
            }
            case CHECKS: {
                FlowPane boxes = new FlowPane(10, 6);
                boxes.setPrefWrapLength(380);
                List<CheckBox> checks = new ArrayList<>();
                List<?> selected = (List<?>) field.initial();
                for (Object option : field.options()) {
                    CheckBox box = new CheckBox(Ui.display(option));
                    box.setUserData(option);
                    box.setSelected(selected.contains(option));
                    checks.add(box);
                }
                boxes.getChildren().addAll(checks);
                readers.put(field.key(), () -> {
                    List<Object> ticked = new ArrayList<>();
                    for (CheckBox box : checks) {
                        if (box.isSelected()) {
                            ticked.add(box.getUserData());
                        }
                    }
                    return ticked;
                });
                return boxes;
            }
            case CHECK: {
                CheckBox box = new CheckBox();
                box.setSelected(Boolean.TRUE.equals(field.initial()));
                readers.put(field.key(), box::isSelected);
                return box;
            }
            default: {
                DatePicker picker = new DatePicker((LocalDate) field.initial());
                readers.put(field.key(), picker::getValue);
                return picker;
            }
        }
    }

    // ---- Shared set-up ----

    private Alert alert(Alert.AlertType type, String title, String message) {
        return alert(type, title, message, true);
    }

    private Alert alert(Alert.AlertType type, String title, String message, boolean show) {
        Alert alert = new Alert(type);
        prepare(alert, title);
        alert.setHeaderText(null);
        Label content = new Label(message);
        content.setWrapText(true);
        content.setMaxWidth(440);
        alert.getDialogPane().setContent(new VBox(content));
        if (show) {
            alert.showAndWait();
        }
        return alert;
    }

    private void prepare(Dialog<?> dialog, String title) {
        dialog.setTitle(title);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        Theme.apply(dialog.getDialogPane());
    }
}
