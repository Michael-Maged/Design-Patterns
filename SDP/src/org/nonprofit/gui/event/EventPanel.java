package org.nonprofit.gui.event;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.nonprofit.event.Attendee;
import org.nonprofit.event.Event;
import org.nonprofit.event.Fundraiser;
import org.nonprofit.event.OutreachProgram;
import org.nonprofit.event.Ticket;
import org.nonprofit.event.Workshop;
import org.nonprofit.gui.common.Ui;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * MVC pattern (view) of the Events screen in the GUI: the events table, the
 * attendees and waitlist of the selected event, their buttons and a bar
 * describing the selected event. It only shows what it is given and reports
 * clicks; {@link EventGuiController} decides what happens.
 */
public class EventPanel extends BorderPane {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TableView<Event> events = Ui.table("events-table", "No events yet.");
    /** One row per registration; the ticket knows its holder and its event. */
    private final TableView<Ticket> attendees = Ui.table("events-attendee-table",
            "Select an event to see who is registered.");
    private final ListView<Attendee> waitlist = new ListView<>();

    private final Button create = Ui.button("Create...", "events-create");
    private final Button edit = Ui.button("Edit...", "events-edit");
    private final Button cancelEvent = Ui.button("Cancel event", "events-cancel");
    private final Button delete = Ui.button("Delete", "events-delete");
    private final CheckBox upcomingOnly = new CheckBox("Upcoming only");
    private final Button remind = Ui.button("Send reminder", "events-remind");
    private final Button remindAll = Ui.button("Reminders for next 24 h", "events-remind-all");
    private final Button report = Ui.button("Attendance report", "events-report");

    private final Button register = Ui.button("Register...", "events-register");
    private final Button checkIn = Ui.button("Check in", "events-check-in");
    private final Button cancelRegistration = Ui.button("Cancel registration", "events-cancel-registration");
    private final Button editContact = Ui.button("Edit contact...", "events-edit-contact");
    private final Button leaveWaitlist = Ui.button("Remove from waitlist", "events-leave-waitlist");

    private final Label selected = new Label();
    private final Label status = Ui.statusBar("events-status");

    public EventPanel() {
        setId("events-screen");
        getStyleClass().add("screen");
        buildTables();

        upcomingOnly.setId("events-upcoming");
        selected.setId("events-selected");
        selected.getStyleClass().add("highlight");
        Label selectedTitle = new Label("Selected event:");
        selectedTitle.getStyleClass().add("info-bar-title");
        HBox selectedRow = new HBox(10, selectedTitle, selected);
        selectedRow.setAlignment(Pos.CENTER_LEFT);
        selectedRow.getStyleClass().add("info-bar");

        VBox top = new VBox(10, Ui.header("Events",
                "Fundraisers, workshops and outreach programs. Create one, register people with ticket add-ons, "
                        + "and everyone registered is told when it changes."),
                selectedRow);

        VBox eventSection = Ui.section("Events",
                Ui.toolbar(create, edit, cancelEvent, delete, Ui.gap(), remind, remindAll, report,
                        Ui.spacer(), upcomingOnly),
                events);
        VBox attendeeSection = Ui.section("Attendees of the selected event",
                Ui.toolbar(register, checkIn, cancelRegistration, Ui.gap(), editContact),
                attendees);
        VBox waitlistSection = Ui.section("Waitlist", Ui.toolbar(leaveWaitlist), waitlist);
        SplitPane people = new SplitPane(attendeeSection, waitlistSection);
        people.setDividerPositions(0.74);

        SplitPane tables = new SplitPane(eventSection, people);
        tables.setOrientation(Orientation.VERTICAL);
        tables.setDividerPositions(0.5);

        setTop(top);
        setCenter(tables);
        setBottom(status);
        BorderPane.setMargin(tables, new Insets(10, 0, 8, 0));
    }

    private void buildTables() {
        events.getColumns().add(Ui.column("ID", 55, Event::getId));
        events.getColumns().add(Ui.column("Type", 80, Event::getType));
        events.getColumns().add(Ui.column("Name", 160, Event::getName));
        events.getColumns().add(Ui.column("Date and time", 115, e -> e.getDateTime().format(DATE_TIME)));
        events.getColumns().add(Ui.column("Location", 120, Event::getLocation));
        events.getColumns().add(Ui.column("Registered", 75, e -> e.getAttendees().size() + "/" + e.getCapacity()));
        events.getColumns().add(Ui.column("Waitlist", 60, e -> e.getWaitlist().size()));
        events.getColumns().add(Ui.column("Ticket", 60, e -> money(e.getBaseTicketPrice())));
        events.getColumns().add(Ui.column("Status / details", 230, EventPanel::detailsOf));

        attendees.getColumns().add(Ui.column("ID", 55, t -> t.getHolder().getId()));
        attendees.getColumns().add(Ui.column("Name", 120, t -> t.getHolder().getName()));
        attendees.getColumns().add(Ui.column("Contact by", 80, t -> t.getHolder().getPreferredContact()));
        attendees.getColumns().add(Ui.column("Ticket", 240, Ticket::description));
        attendees.getColumns().add(Ui.column("Price", 65, t -> money(t.price())));
        attendees.getColumns().add(Ui.column("Checked in", 75,
                t -> t.getEvent().getCheckedIn().contains(t.getHolder()) ? "Yes" : "No"));

        waitlist.setId("events-waitlist");
        waitlist.setPlaceholder(new Label("Nobody is waiting."));
        waitlist.setCellFactory(list -> new ListCell<Attendee>() {
            @Override
            protected void updateItem(Attendee attendee, boolean empty) {
                super.updateItem(attendee, empty);
                setText(empty || attendee == null ? null : (getIndex() + 1) + ". " + attendee);
            }
        });
    }

    /** "CANCELLED", or the type's own details, e.g. a fundraiser's progress. */
    private static String detailsOf(Event event) {
        if (event.isCancelled()) {
            return "CANCELLED";
        }
        if (event instanceof Fundraiser f) {
            return String.format(Locale.ROOT, "Goal %s, raised %s (%.0f%%)",
                    money(f.getFundraisingGoal()), money(f.getAmountRaised()), f.progressPercent());
        }
        if (event instanceof Workshop w) {
            return "Topic: " + w.getTopic() + ", instructor: " + w.getInstructor();
        }
        if (event instanceof OutreachProgram o) {
            return "For: " + o.getTargetCommunity();
        }
        return "";
    }

    static String money(double amount) {
        return String.format(Locale.ROOT, "$%.2f", amount);
    }

    // ---- What the controller shows ----

    public void showEvents(List<Event> rows) {
        Ui.show(events, rows, Event::getId);
        events.refresh();
    }

    /** The registrations of the selected event (empty when none is selected). */
    public void showAttendees(List<Ticket> rows) {
        Ui.show(attendees, rows, t -> t.getHolder().getId());
        attendees.refresh();
    }

    /** The people waiting, first in line first. */
    public void showWaitlist(List<Attendee> rows) {
        Attendee before = waitlist.getSelectionModel().getSelectedItem();
        waitlist.getItems().setAll(rows);
        if (before != null && rows.contains(before)) {
            waitlist.getSelectionModel().select(before);
        }
    }

    public void showSelectedEvent(String text) {
        selected.setText(text);
    }

    public void showStatus(String text) {
        status.setText(text);
    }

    // ---- What the user selected ----

    /** @return the selected event, or null */
    public Event selectedEvent() {
        return events.getSelectionModel().getSelectedItem();
    }

    /** @return the attendee of the selected registration row, or null */
    public Attendee selectedAttendee() {
        Ticket ticket = attendees.getSelectionModel().getSelectedItem();
        return ticket == null ? null : ticket.getHolder();
    }

    /** @return the selected person on the waitlist, or null */
    public Attendee selectedWaiting() {
        return waitlist.getSelectionModel().getSelectedItem();
    }

    public boolean isUpcomingOnly() {
        return upcomingOnly.isSelected();
    }

    public boolean selectEvent(String id) {
        return Ui.select(events, Event::getId, id);
    }

    public boolean selectAttendee(String id) {
        return Ui.select(attendees, t -> t.getHolder().getId(), id);
    }

    public boolean selectWaiting(String id) {
        for (Attendee attendee : waitlist.getItems()) {
            if (attendee.getId().equals(id)) {
                waitlist.getSelectionModel().select(attendee);
                return true;
            }
        }
        return false;
    }

    // ---- What the user can do (the controller says what each one means) ----

    public void onCreate(Runnable action) { create.setOnAction(e -> action.run()); }
    public void onEdit(Runnable action) { edit.setOnAction(e -> action.run()); }
    public void onCancelEvent(Runnable action) { cancelEvent.setOnAction(e -> action.run()); }
    public void onDelete(Runnable action) { delete.setOnAction(e -> action.run()); }
    public void onRemind(Runnable action) { remind.setOnAction(e -> action.run()); }
    public void onRemindAll(Runnable action) { remindAll.setOnAction(e -> action.run()); }
    public void onReport(Runnable action) { report.setOnAction(e -> action.run()); }

    public void onRegister(Runnable action) { register.setOnAction(e -> action.run()); }
    public void onCheckIn(Runnable action) { checkIn.setOnAction(e -> action.run()); }
    public void onCancelRegistration(Runnable action) { cancelRegistration.setOnAction(e -> action.run()); }
    public void onEditContact(Runnable action) { editContact.setOnAction(e -> action.run()); }
    public void onLeaveWaitlist(Runnable action) { leaveWaitlist.setOnAction(e -> action.run()); }

    /** Called when the "Upcoming only" box is ticked or cleared. */
    public void onUpcomingChanged(Runnable action) { upcomingOnly.setOnAction(e -> action.run()); }

    /** Called whenever another event is selected (or none). */
    public void onEventSelected(Runnable action) {
        events.getSelectionModel().selectedItemProperty().addListener((property, old, now) -> action.run());
    }
}
