package org.nonprofit.gui.event;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.event.Attendee;
import org.nonprofit.event.Event;
import org.nonprofit.event.EventService;
import org.nonprofit.event.EventType;
import org.nonprofit.event.Fundraiser;
import org.nonprofit.event.OutreachProgram;
import org.nonprofit.event.RegistrationResult;
import org.nonprofit.event.Ticket;
import org.nonprofit.event.TicketAddOn;
import org.nonprofit.event.Workshop;
import org.nonprofit.gui.common.Dialogs;
import org.nonprofit.gui.common.Feed;
import org.nonprofit.gui.common.Form;
import org.nonprofit.gui.common.Inputs;
import org.nonprofit.gui.common.ScreenActions;
import org.nonprofit.gui.common.Ui;

/**
 * MVC pattern (controller) of the Events screen in the GUI: turns each click
 * on the {@link EventPanel} into calls on the model ({@link EventService},
 * unchanged from the console version) and tells the panel what to show.
 * Rules broken in the model come back as exceptions and are shown as error
 * dialogs by {@link ScreenActions}.
 *
 * Events are made by the model's SIMPLE FACTORY (the type drop-down), each
 * ticket add-on is a DECORATOR on the ticket, and the attendees are the
 * events' OBSERVERS: after every action their new inbox notes are posted to
 * the notification feed, as the console screen prints them.
 */
public class EventGuiController {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String NEW_PERSON = "New person (fill in below)";
    /** A ticket worth nothing, so wrapping it in one decorator shows that add-on's own text and price. */
    private static final Ticket NO_TICKET = new Ticket() {
        @Override public double price() { return 0; }
        @Override public String description() { return ""; }
        @Override public Event getEvent() { return null; }
        @Override public Attendee getHolder() { return null; }
    };

    private final EventService service;
    private final EventPanel panel;
    private final Dialogs dialogs;
    private final Feed feed;
    private final ScreenActions actions;

    /**
     * @param deliveries the messages Communication sent since the last call
     *                   ({@code CommunicationService::takeOutput}); this module does not import Communication
     */
    public EventGuiController(EventService service, Dialogs dialogs, Feed feed, Supplier<List<String>> deliveries) {
        this.service = service;
        this.dialogs = dialogs;
        this.feed = feed;
        this.panel = new EventPanel();
        this.actions = new ScreenActions(dialogs, feed, deliveries, this::refresh);

        panel.onCreate(() -> run(this::createEvent));
        panel.onEdit(() -> run(this::editEvent));
        panel.onCancelEvent(() -> run(this::cancelEvent));
        panel.onDelete(() -> run(this::deleteEvent));
        panel.onRemind(() -> run(this::sendReminder));
        panel.onRemindAll(() -> run(this::sendReminders));
        panel.onReport(() -> run(this::attendanceReport));
        panel.onUpcomingChanged(() -> run(this::filterChanged));

        panel.onRegister(() -> run(this::register));
        panel.onCheckIn(() -> run(this::checkIn));
        panel.onCancelRegistration(() -> run(this::cancelRegistration));
        panel.onLeaveWaitlist(() -> run(this::leaveWaitlist));
        panel.onEditContact(() -> run(this::editContact));

        panel.onEventSelected(this::showSelectedEvent);
        refresh();
    }

    public EventPanel getPanel() {
        return panel;
    }

    /** Reloads the events table and the selected event's attendees and waitlist from the service. */
    public void refresh() {
        // Reloading the events briefly clears their selection (and so the attendees shown); keep the person selected.
        Attendee attendee = panel.selectedAttendee();
        Attendee waiting = panel.selectedWaiting();
        panel.showEvents(panel.isUpcomingOnly() ? service.upcomingEvents(LocalDateTime.now()) : service.listEvents());
        showSelectedEvent();
        if (attendee != null) {
            panel.selectAttendee(attendee.getId());
        }
        if (waiting != null) {
            panel.selectWaiting(waiting.getId());
        }
    }

    private void showSelectedEvent() {
        Event event = panel.selectedEvent();
        if (event == null) {
            panel.showAttendees(List.of());
            panel.showWaitlist(List.of());
            panel.showSelectedEvent("None - select an event in the table.");
            return;
        }
        panel.showAttendees(new ArrayList<>(event.getRegistrations().values()));
        panel.showWaitlist(event.getWaitlist());
        panel.showSelectedEvent(event.summary() + " | tickets from " + EventPanel.money(event.getBaseTicketPrice()));
    }

    /** Every button: the shared handling, then the attendees' new notes (Observer) go to the feed. */
    private void run(Runnable action) {
        actions.run(action);
        for (Attendee attendee : service.listAttendees()) {
            for (String note : attendee.takeNewNotes()) {
                feed.post("[inbox of " + attendee.getName() + "] " + note);
            }
        }
    }

    // ---- Events ----

    private void createEvent() {
        Form form = new Form("Create an event")
                .header("The EventFactory builds the right kind of event for the type you choose.")
                .choice("type", "Type", List.of(EventType.values()), EventType.FUNDRAISER)
                .text("name", "Name", "")
                .date("date", "Date", LocalDate.now().plusDays(7))
                .text("time", "Time", "18:00", "18:30")
                .text("location", "Location", "")
                .text("capacity", "Capacity", "", "50");
        dialogs.form(form).ifPresent(values -> {
            LocalDateTime when = dateTime(values.date("date"), values.text("time"));
            int capacity = Inputs.whole(values.text("capacity"), "Capacity");
            Event event = service.createEvent(values.choice("type", EventType.class), values.text("name"), when,
                    values.text("location"), capacity);
            refresh(); // so the new row is in the table and can be selected
            panel.selectEvent(event.getId());
            String type = Ui.display(event.getType());
            panel.showStatus("EventFactory created " + ("AEIOU".indexOf(type.charAt(0)) >= 0 ? "an " : "a ") + type
                    + " " + event.getId() + ": " + event.getName() + ".");
        });
    }

    /**
     * One form for every detail, plus the type's own fields. Only what was
     * changed is sent to the service. The changes that can be undone (name,
     * price, type details) go first and are put back if a later step is
     * refused, so the edit is all or nothing; a new place or time (which
     * tells the attendees) and a pledge (which adds up) go last, after their
     * input is checked.
     */
    private void editEvent() {
        Event event = requireSelectedEvent();
        if (event.isCancelled()) {
            throw new IllegalStateException(event.getName() + " is cancelled - it cannot be edited");
        }
        Form form = new Form("Edit " + event)
                .header(Ui.display(event.getType()) + ". People registered are told about a new date, time or place.")
                .text("name", "Name", event.getName())
                .date("date", "Date", event.getDateTime().toLocalDate())
                .text("time", "Time", event.getDateTime().format(TIME), "18:30")
                .text("location", "Location", event.getLocation())
                .text("capacity", "Capacity", String.valueOf(event.getCapacity()))
                .text("price", "Base ticket price", amount(event.getBaseTicketPrice()));
        if (event instanceof Fundraiser fundraiser) {
            form.text("goal", "Fundraising goal", amount(fundraiser.getFundraisingGoal()))
                    .text("pledge", "Record a pledge", "", "250 (optional)");
        } else if (event instanceof Workshop workshop) {
            form.text("topic", "Topic", workshop.getTopic())
                    .text("instructor", "Instructor", workshop.getInstructor());
        } else if (event instanceof OutreachProgram outreach) {
            form.text("community", "Target community", outreach.getTargetCommunity());
        }
        dialogs.form(form).ifPresent(values -> {
            String id = event.getId();
            // Everything typed is read first, so bad input changes nothing.
            String name = values.text("name");
            LocalDateTime when = dateTime(values.date("date"), values.text("time"));
            String location = values.text("location");
            if (location.isEmpty()) {
                throw new IllegalArgumentException("Event location is required");
            }
            int capacity = Inputs.whole(values.text("capacity"), "Capacity");
            double price = Inputs.number(values.text("price"), "Base ticket price");
            Double goal = null;
            Double pledge = null;
            if (event instanceof Fundraiser) {
                goal = Inputs.number(values.text("goal"), "Fundraising goal");
                if (!values.text("pledge").isEmpty()) {
                    pledge = Inputs.number(values.text("pledge"), "Pledge");
                    if (pledge <= 0) {
                        throw new IllegalArgumentException("Pledge must be a positive number");
                    }
                }
            }

            List<String> changed = new ArrayList<>();
            String oldName = event.getName();
            double oldPrice = event.getBaseTicketPrice();
            Fundraiser fundraiser = event instanceof Fundraiser f ? f : null;
            double oldGoal = fundraiser == null ? 0 : fundraiser.getFundraisingGoal();
            Workshop workshop = event instanceof Workshop w ? w : null;
            String oldTopic = workshop == null ? null : workshop.getTopic();
            String oldInstructor = workshop == null ? null : workshop.getInstructor();
            OutreachProgram outreach = event instanceof OutreachProgram o ? o : null;
            String oldCommunity = outreach == null ? null : outreach.getTargetCommunity();
            try {
                if (!name.equals(oldName)) {
                    service.renameEvent(id, name);
                    changed.add("name");
                }
                if (price != oldPrice) {
                    service.setBaseTicketPrice(id, price);
                    changed.add("base ticket price (new tickets only)");
                }
                if (fundraiser != null && goal != oldGoal) {
                    service.setFundraiserGoal(id, goal);
                    changed.add("goal");
                }
                if (workshop != null && (!values.text("topic").equals(oldTopic)
                        || !values.text("instructor").equals(oldInstructor))) {
                    service.setWorkshopDetails(id, values.text("topic"), values.text("instructor"));
                    changed.add("topic and instructor");
                }
                if (outreach != null && !values.text("community").equals(oldCommunity)) {
                    service.setTargetCommunity(id, values.text("community"));
                    changed.add("target community");
                }
                if (capacity != event.getCapacity()) {
                    service.setCapacity(id, capacity);
                    changed.add("capacity");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                // Putting the old values back is always allowed (the event is not cancelled).
                service.renameEvent(id, oldName);
                service.setBaseTicketPrice(id, oldPrice);
                if (fundraiser != null) {
                    service.setFundraiserGoal(id, oldGoal);
                }
                if (workshop != null) {
                    service.setWorkshopDetails(id, oldTopic, oldInstructor);
                }
                if (outreach != null) {
                    service.setTargetCommunity(id, oldCommunity);
                }
                throw new IllegalStateException(e.getMessage() + ". Nothing was changed.");
            }
            if (pledge != null) {
                service.recordPledge(id, pledge);
                changed.add("pledge of " + EventPanel.money(pledge) + " recorded");
            }
            if (!location.equals(event.getLocation())) {
                service.relocate(id, location);
                changed.add("location");
            }
            if (!when.equals(event.getDateTime())) {
                service.reschedule(id, when);
                changed.add("date and time");
            }
            panel.showStatus(changed.isEmpty() ? "Nothing was changed."
                    : "Updated " + event + ": " + String.join(", ", changed) + ".");
        });
    }

    private void cancelEvent() {
        Event event = requireSelectedEvent();
        int registered = event.getAttendees().size();
        int waiting = event.getWaitlist().size();
        if (dialogs.confirm("Cancel " + event + "? The " + registered + " registered and " + waiting
                + " waitlisted people are told, and it cannot be undone.")) {
            service.cancelEvent(event.getId());
            panel.showStatus(event + " is cancelled; " + registered + " registered and " + waiting
                    + " waitlisted people were told.");
        }
    }

    private void deleteEvent() {
        Event event = requireSelectedEvent();
        if (dialogs.confirm("Delete " + event + "? It is removed from the system.")) {
            service.deleteEvent(event.getId());
            panel.showStatus(event + " is deleted.");
        }
    }

    private void sendReminder() {
        Event event = requireSelectedEvent();
        service.sendReminder(event.getId(), LocalDateTime.now());
        panel.showStatus("Reminder sent to the " + event.getAttendees().size() + " people registered for "
                + event.getName() + ".");
    }

    private void sendReminders() {
        List<Event> reminded = service.sendUpcomingReminders(LocalDateTime.now(), Duration.ofHours(24));
        panel.showStatus(reminded.isEmpty() ? "No events in the next 24 hours."
                : "Reminders sent for " + Ui.display(reminded) + ".");
    }

    private void attendanceReport() {
        Event event = requireSelectedEvent();
        dialogs.text("Attendance report - " + event.getName(), service.attendanceReport(event.getId()));
    }

    private void filterChanged() {
        panel.showStatus(panel.isUpcomingOnly() ? "Showing upcoming events only (not cancelled, not started)."
                : "Showing all events.");
    }

    // ---- Attendees ----

    /**
     * Everything is checked (event, name, contact method) before a new
     * person is put on file, and they are stored only once the registration
     * has gone through, so a refused registration leaves no stray attendee
     * behind (the same rule as the console screen).
     */
    private void register() {
        Event event = requireSelectedEvent();
        if (event.isCancelled()) {
            throw new IllegalStateException(event.getName() + " is cancelled");
        }
        if (!event.getDateTime().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException(event.getName() + " has already started - registration is closed");
        }
        List<Object> people = new ArrayList<>();
        people.add(NEW_PERSON);
        people.addAll(service.listAttendees());
        Form form = new Form("Register for " + event)
                .header("Ticket prices (each add-on wraps the ticket and adds to it):\n" + priceList(event)
                        + "\nPick a person on file, or New person and fill in their details.")
                .choice("person", "Attendee", people, NEW_PERSON)
                .text("name", "New person: name", "")
                .text("email", "New person: e-mail", "")
                .text("phone", "New person: phone", "")
                .choice("contact", "New person: contact by", List.of(ContactPreference.values()),
                        ContactPreference.EMAIL)
                .checks("addOns", "Add-ons", List.of(TicketAddOn.values()), List.of());
        dialogs.form(form).ifPresent(values -> {
            Object who = values.choice("person", Object.class);
            if (who == null) {
                throw new IllegalArgumentException("Choose a person on file or New person");
            }
            TicketAddOn[] addOns = values.checked("addOns", TicketAddOn.class).toArray(new TicketAddOn[0]);
            boolean isNew = !(who instanceof Attendee);
            Attendee attendee;
            if (isNew) {
                ContactPreference contact = values.choice("contact", ContactPreference.class);
                if (contact == null) {
                    throw new IllegalArgumentException("Contact method is required");
                }
                attendee = new Attendee(values.text("name"), Inputs.orNull(values.text("email")),
                        Inputs.orNull(values.text("phone")), contact);
            } else {
                attendee = (Attendee) who;
            }
            RegistrationResult result = service.register(event.getId(), attendee, LocalDateTime.now(), addOns);
            if (isNew) {
                service.addAttendee(attendee);
            }
            String newId = isNew ? " New attendee id: " + attendee.getId() + "." : "";
            switch (result.status()) {
                case REGISTERED:
                    refresh();
                    panel.selectAttendee(attendee.getId());
                    panel.showStatus("Registered " + attendee + " for " + event.getName() + ": "
                            + result.ticket().description() + " = " + EventPanel.money(result.ticket().price())
                            + "." + newId);
                    break;
                case WAITLISTED:
                    refresh();
                    panel.selectWaiting(attendee.getId());
                    panel.showStatus(event.getName() + " is full; " + attendee + " is number "
                            + result.waitlistPosition() + " on the waitlist (their add-ons are kept for when a seat"
                            + " opens)." + newId);
                    break;
                default:
                    panel.showStatus(attendee + " is " + Ui.display(result.status()).toLowerCase(Locale.ROOT)
                            + " for " + event.getName() + ".");
            }
        });
    }

    /** "General admission $100.00 | + VIP seating $50.00 | ...": each add-on applied on its own. */
    private static String priceList(Event event) {
        StringBuilder text = new StringBuilder("General admission " + EventPanel.money(event.getBaseTicketPrice()));
        for (TicketAddOn addOn : TicketAddOn.values()) {
            Ticket extra = addOn.applyTo(NO_TICKET);
            text.append("  |  ").append(extra.description().trim()).append(' ').append(EventPanel.money(extra.price()));
        }
        return text.toString();
    }

    private void checkIn() {
        Event event = requireSelectedEvent();
        Attendee attendee = requireSelectedAttendee();
        service.checkIn(event.getId(), attendee);
        panel.showStatus(attendee + " checked in to " + event.getName() + ".");
    }

    private void cancelRegistration() {
        Event event = requireSelectedEvent();
        Attendee attendee = requireSelectedAttendee();
        if (!dialogs.confirm("Cancel the registration of " + attendee + " for " + event.getName()
                + "? The first person on the waitlist gets the seat.")) {
            return;
        }
        List<Attendee> waitingBefore = event.getWaitlist();
        if (service.cancelRegistration(event.getId(), attendee)) {
            List<String> promoted = new ArrayList<>();
            for (Attendee person : waitingBefore) {
                if (event.getAttendees().contains(person)) {
                    promoted.add(person.toString());
                }
            }
            panel.showStatus(attendee + " is no longer registered for " + event.getName() + "."
                    + (promoted.isEmpty() ? "" : " Moved up from the waitlist: " + String.join(", ", promoted) + "."));
        } else {
            throw new IllegalArgumentException(attendee + " was not registered for " + event.getName());
        }
    }

    private void leaveWaitlist() {
        Event event = requireSelectedEvent();
        Attendee attendee = panel.selectedWaiting();
        if (attendee == null) {
            throw new IllegalArgumentException("Select a person on the waitlist first.");
        }
        if (dialogs.confirm("Remove " + attendee + " from the waitlist of " + event.getName() + "?")
                && service.cancelRegistration(event.getId(), attendee)) {
            panel.showStatus(attendee + " is no longer on the waitlist of " + event.getName() + ".");
        }
    }

    /** The selected attendee (or waitlisted person); with none selected, the person is picked from everyone on file. */
    private void editContact() {
        Attendee selected = panel.selectedAttendee() != null ? panel.selectedAttendee() : panel.selectedWaiting();
        if (selected == null) {
            List<Attendee> onFile = service.listAttendees();
            if (onFile.isEmpty()) {
                throw new IllegalStateException("No attendees on file yet");
            }
            Form pick = new Form("Edit an attendee's contact").choice("person", "Attendee", onFile, onFile.get(0));
            selected = dialogs.form(pick).map(values -> values.choice("person", Attendee.class)).orElse(null);
            if (selected == null) {
                return;
            }
        }
        Attendee attendee = selected;
        Form form = new Form("Edit " + attendee)
                .text("name", "Name", attendee.getName())
                .text("email", "E-mail", attendee.getEmail())
                .text("phone", "Phone", attendee.getPhone())
                .choice("contact", "Contact by", List.of(ContactPreference.values()), attendee.getPreferredContact());
        dialogs.form(form).ifPresent(values -> {
            ContactPreference contact = values.choice("contact", ContactPreference.class);
            if (contact == null) {
                throw new IllegalArgumentException("Contact method is required");
            }
            attendee.updateContact(values.text("name"), Inputs.orNull(values.text("email")),
                    Inputs.orNull(values.text("phone")), contact);
            panel.showStatus("Updated the contact details of " + attendee + ".");
        });
    }

    // ---- Helpers ----

    /** A date from the date picker and a time typed as "18:30". */
    private static LocalDateTime dateTime(LocalDate date, String time) {
        if (date == null) {
            throw new IllegalArgumentException("Event date is required");
        }
        try {
            return date.atTime(LocalTime.parse(time.trim(), TIME));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Time must look like 18:30");
        }
    }

    /** "100" or "12.50", as typed back into a form. */
    private static String amount(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.2f", value);
    }

    private Event requireSelectedEvent() {
        Event event = panel.selectedEvent();
        if (event == null) {
            throw new IllegalArgumentException("Select an event in the table first.");
        }
        return event;
    }

    private Attendee requireSelectedAttendee() {
        Attendee attendee = panel.selectedAttendee();
        if (attendee == null) {
            throw new IllegalArgumentException("Select an attendee in the attendees table first.");
        }
        return attendee;
    }
}
