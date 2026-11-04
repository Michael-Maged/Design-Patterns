package org.nonprofit.gui.communication;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.nonprofit.communication.model.SentMessage;
import org.nonprofit.core.model.ContactPreference;
import org.nonprofit.gui.common.Ui;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * MVC pattern (view) of the Communication screen in the GUI: the compose
 * area (who, subject, text, urgent, channel), the preview of the text on
 * each channel and the message log. It only shows what it is given and
 * reports clicks; {@link CommunicationGuiController} decides what happens.
 */
public class CommunicationPanel extends BorderPane {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Who the message goes to. */
    public enum Recipients { GROUP, PERSON, EVENT }

    private final ToggleGroup recipients = new ToggleGroup();
    private final RadioButton toGroup = radio("A whole group", "communication-to-group", Recipients.GROUP);
    private final RadioButton toPerson = radio("One person", "communication-to-person", Recipients.PERSON);
    private final RadioButton toEvent = radio("Attendees of one event", "communication-to-event", Recipients.EVENT);
    private final ComboBox<String> group = new ComboBox<>();
    private final TextField personId = new TextField();
    private final TextField eventId = new TextField();
    private final TextField subject = new TextField();
    private final TextArea text = new TextArea();
    private final CheckBox urgent = new CheckBox("Urgent");
    private final ComboBox<String> channel = new ComboBox<>();
    private final List<ContactPreference> channels;

    private final Button send = Ui.button("Send", "communication-send");
    private final Button preview = Ui.button("Preview", "communication-preview");
    private final Button clear = Ui.button("Clear", "communication-clear");

    private final TextArea previewText = new TextArea();
    private final TableView<SentMessage> log = Ui.table("communication-log-table", "No messages sent yet.");
    private final Label logCount = new Label();
    private final Label status = Ui.statusBar("communication-status");

    /**
     * @param groups   the group names offered for a broadcast, e.g. "Donors"
     * @param channels the channels that can be forced on everyone; the first choice is always each person's preference
     */
    public CommunicationPanel(List<String> groups, List<ContactPreference> channels) {
        setId("communication-screen");
        getStyleClass().add("screen");
        this.channels = List.copyOf(channels);

        group.setId("communication-group");
        group.getItems().setAll(groups);
        if (!groups.isEmpty()) {
            group.setValue(groups.get(0));
        }
        personId.setId("communication-person-id");
        personId.setPromptText("DNR-1, VOL-1, ATT-1 or BEN-1");
        eventId.setId("communication-event-id");
        eventId.setPromptText("EVT-1");
        subject.setId("communication-subject");
        text.setId("communication-text");
        text.setWrapText(true);
        text.setPrefRowCount(3);
        urgent.setId("communication-urgent");
        channel.setId("communication-channel");
        channel.getItems().add("Each person's preference");
        for (ContactPreference preference : this.channels) {
            channel.getItems().add(Ui.display(preference));
        }
        channel.getSelectionModel().select(0);

        // Only the field of the chosen recipient kind can be typed in.
        group.disableProperty().bind(toGroup.selectedProperty().not());
        personId.disableProperty().bind(toPerson.selectedProperty().not());
        eventId.disableProperty().bind(toEvent.selectedProperty().not());
        toGroup.setSelected(true);

        previewText.setId("communication-preview-text");
        previewText.setEditable(false);
        previewText.setWrapText(true);
        previewText.getStyleClass().add("mono");
        previewText.setPromptText("Write a message and press Preview to see how each channel dresses it.");
        logCount.setId("communication-log-count");
        logCount.getStyleClass().add("muted");
        buildLog();

        VBox composeSection = Ui.section("Compose", Ui.toolbar(send, preview, Ui.gap(), clear), composeGrid());
        composeSection.setPrefWidth(520);
        composeSection.setMinWidth(440);
        Label previewNote = new Label("The same text on each channel (Decorator: urgent tag, signature, footer)");
        previewNote.getStyleClass().add("muted");
        VBox previewSection = Ui.section("Preview", Ui.toolbar(previewNote), previewText);
        HBox.setHgrow(previewSection, Priority.ALWAYS);
        HBox top = new HBox(10, composeSection, previewSection);

        VBox logSection = Ui.section("Message log", Ui.toolbar(logCount), log);
        SplitPane parts = new SplitPane(top, logSection);
        parts.setOrientation(Orientation.VERTICAL);
        parts.setDividerPositions(0.55);

        setTop(Ui.header("Communication",
                "Send a message to a group, one person or an event's attendees. "
                        + "Each person gets it on their preferred channel unless you pick one for everyone."));
        setCenter(parts);
        setBottom(status);
        BorderPane.setMargin(parts, new Insets(10, 0, 8, 0));
    }

    private RadioButton radio(String label, String id, Recipients kind) {
        RadioButton button = new RadioButton(label);
        button.setId(id);
        button.setUserData(kind);
        button.setToggleGroup(recipients);
        button.setMnemonicParsing(false);
        return button;
    }

    private GridPane composeGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, fields);

        GridPane who = new GridPane();
        who.setHgap(10);
        who.setVgap(6);
        who.addRow(0, toGroup, group);
        who.addRow(1, toPerson, personId);
        who.addRow(2, toEvent, eventId);
        group.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(group, Priority.ALWAYS);

        Label channelTitle = new Label("Channel (Strategy):");
        channelTitle.getStyleClass().add("info-bar-title");
        HBox options = new HBox(14, urgent, channelTitle, channel);
        options.setAlignment(Pos.CENTER_LEFT);

        grid.addRow(0, formLabel("Send to"), who);
        grid.addRow(1, formLabel("Subject"), subject);
        grid.addRow(2, formLabel("Message"), text);
        grid.addRow(3, formLabel("Options"), options);
        return grid;
    }

    private static Label formLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        GridPane.setValignment(label, VPos.TOP);
        label.setPadding(new Insets(4, 0, 0, 0));
        return label;
    }

    private void buildLog() {
        log.getColumns().add(Ui.column("Time", 140, m -> m.getTime().format(TIME)));
        log.getColumns().add(Ui.column("Channel", 90, SentMessage::getChannel));
        log.getColumns().add(Ui.column("Recipient", 160, SentMessage::getRecipient));
        log.getColumns().add(Ui.column("Subject", 300, SentMessage::getSubject));
    }

    // ---- What the controller shows ----

    /** The log, oldest first; scrolls to the newest message. */
    public void showLog(List<SentMessage> rows) {
        log.getItems().setAll(rows);
        if (!rows.isEmpty()) {
            log.scrollTo(rows.size() - 1);
        }
        logCount.setText(rows.size() + " message(s) sent, newest at the bottom.");
    }

    public void showPreview(String text) {
        previewText.setText(text);
    }

    public void showStatus(String text) {
        status.setText(text);
    }

    /** Empties the compose fields (the recipient choice stays). */
    public void clearMessage() {
        subject.clear();
        text.clear();
        urgent.setSelected(false);
        channel.getSelectionModel().select(0);
        previewText.clear();
    }

    // ---- What the user entered ----

    public Recipients recipients() {
        return (Recipients) recipients.getSelectedToggle().getUserData();
    }

    /** @return the chosen group name, or null */
    public String group() {
        return group.getValue();
    }

    public String personId() {
        return personId.getText();
    }

    public String eventId() {
        return eventId.getText();
    }

    public String subject() {
        return subject.getText();
    }

    public String text() {
        return text.getText();
    }

    public boolean urgent() {
        return urgent.isSelected();
    }

    /** @return the channel chosen for everyone, or null for each person's preference */
    public ContactPreference forcedChannel() {
        int index = channel.getSelectionModel().getSelectedIndex();
        return index <= 0 ? null : channels.get(index - 1);
    }

    // ---- What the user can do (the controller says what each one means) ----

    public void onSend(Runnable action) { send.setOnAction(e -> action.run()); }
    public void onPreview(Runnable action) { preview.setOnAction(e -> action.run()); }
    public void onClear(Runnable action) { clear.setOnAction(e -> action.run()); }
}
