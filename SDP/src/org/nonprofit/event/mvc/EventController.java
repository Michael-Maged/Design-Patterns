package org.nonprofit.event.mvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.nonprofit.core.model.ContactPreference;
import org.nonprofit.event.observer.Attendee;
import org.nonprofit.event.observer.Event;
import org.nonprofit.event.model.EventService;
import org.nonprofit.event.simplefactory.EventType;
import org.nonprofit.event.model.RegistrationResult;
import org.nonprofit.event.decorator.TicketAddOn;

/**
 * MVC pattern (controller): takes the user's menu choice from the
 * {@link EventView}, calls the model ({@link EventService} and the events,
 * tickets and attendees it manages) and tells the view what to show.
 * The view never talks to the model directly, and the model knows nothing about menus.
 *
 * After every action the view shows the messages the Communication module
 * sent (passed in as {@code deliveries}, so this module does not import
 * Communication) and any new notes in the attendees' inboxes.
 * IDs can be typed in any case ("evt-1" finds EVT-1).
 */
public class EventController {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EventService service;
    private final EventView view;
    /** True when opened from the main menu: option 0 goes "Back" instead of ending the program. */
    private final boolean embedded;
    /** Gives the delivery lines recorded since the last call (none when nothing sends messages). */
    private final Supplier<List<String>> deliveries;

    /**
     * @param embedded   true when another menu opens this screen, so 0 means "Back"
     * @param deliveries where to take the messages sent since the last action
     *                   (e.g. {@code CommunicationService::takeOutput})
     */
    public EventController(EventService service, EventView view, boolean embedded,
            Supplier<List<String>> deliveries) {
        this.service = service;
        this.view = view;
        this.embedded = embedded;
        this.deliveries = deliveries;
    }

    /** Shows the menu again after every action until the user chooses Exit (or Back). */
    public void run() {
        boolean running = true;
        while (running) {
            int choice = view.showMenu(embedded ? "Back" : "Exit");
            try {
                switch (choice) {
                    case 1: view.showList(summaries(service.listEvents()), "No events yet."); break;
                    case 2: view.showList(summaries(service.upcomingEvents(LocalDateTime.now())), "No upcoming events."); break;
                    case 3: createEvent(); break;
                    case 4: editEvent(); break;
                    case 5: registerAttendee(); break;
                    case 6: cancelRegistration(); break;
                    case 7: checkIn(); break;
                    case 8: updateAttendee(); break;
                    case 9: sendReminder(); break;
                    case 10: sendReminders(); break;
                    case 11: view.showText(service.attendanceReport(askId("Event id (e.g. EVT-1)"))); break;
                    case 12: cancelEvent(); break;
                    case 13: deleteEvent(); break;
                    case 0: running = false; break;
                    default: view.showError("Please choose a number from the menu.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                // Rules broken in the model (event full, cancelled, ...) are shown, not thrown.
                view.showError(e.getMessage());
            }
            showNewMessages();
        }
        if (!embedded) {
            view.showMessage("Goodbye.");
        }
    }

    /** Shows the messages sent and the attendee notes received since the last action. */
    private void showNewMessages() {
        view.showDeliveries(deliveries.get());
        for (Attendee attendee : service.listAttendees()) {
            view.showInbox(attendee.getName(), attendee.takeNewNotes());
        }
    }

    // ---- Events ----

    private void createEvent() {
        String type = view.ask("Type (fundraiser / workshop / outreach)");
        String name = view.ask("Name");
        LocalDateTime when = parseDateTime(view.ask("Date and time (yyyy-MM-dd HH:mm)"));
        String location = view.ask("Location");
        int capacity = parseInt(view.ask("Capacity"), "Capacity");
        Event event = service.createEvent(type, name, when, location, capacity);
        view.showMessage("Created " + event.summary());
    }

    private void editEvent() {
        Event event = requireEvent(askId("Event id (e.g. EVT-1)"));
        String id = event.getId();
        switch (view.showEditMenu(event.toString())) {
            case 1: service.renameEvent(id, view.ask("New name")); break;
            case 2: service.reschedule(id, parseDateTime(view.ask("New date and time (yyyy-MM-dd HH:mm)"))); break;
            case 3: service.relocate(id, view.ask("New location")); break;
            case 4: service.setCapacity(id, parseInt(view.ask("New capacity"), "Capacity")); break;
            case 5:
                service.setBaseTicketPrice(id, parseDouble(view.ask("New base ticket price"), "Price"));
                view.showMessage(String.format("New tickets for %s now start at $%.2f.",
                        event.getName(), event.getBaseTicketPrice()));
                return;
            case 6:
                requireType(event, EventType.FUNDRAISER);
                service.setFundraiserGoal(id, parseDouble(view.ask("Fundraising goal"), "Goal"));
                break;
            case 7:
                requireType(event, EventType.FUNDRAISER);
                service.recordPledge(id, parseDouble(view.ask("Pledge amount"), "Pledge"));
                break;
            case 8:
                requireType(event, EventType.WORKSHOP);
                service.setWorkshopDetails(id, view.ask("Topic"), view.ask("Instructor"));
                break;
            case 9:
                requireType(event, EventType.OUTREACH);
                service.setTargetCommunity(id, view.ask("Target community"));
                break;
            case 0: return;
            default:
                view.showError("Please choose a number from the menu.");
                return;
        }
        view.showMessage("Updated " + event.summary());
    }

    /** Checked before asking for the value, so the user is not asked for something that cannot apply. */
    private static void requireType(Event event, EventType type) {
        if (event.getType() != type) {
            throw new IllegalArgumentException(event + " is a " + event.getType() + " event; this option is for "
                    + type + " events only");
        }
    }

    private void sendReminder() {
        Event event = requireEvent(askId("Event id"));
        service.sendReminder(event.getId(), LocalDateTime.now());
        view.showMessage("Reminder sent to the " + event.getAttendees().size() + " people registered for "
                + event.getName() + ".");
    }

    private void sendReminders() {
        List<Event> reminded = service.sendUpcomingReminders(LocalDateTime.now(), Duration.ofHours(24));
        view.showMessage(reminded.isEmpty() ? "No events in the next 24 hours."
                : "Reminders sent for " + reminded + ".");
    }

    private void cancelEvent() {
        String eventId = askId("Event id to cancel");
        service.cancelEvent(eventId);
        view.showMessage(eventId + " is cancelled.");
    }

    private void deleteEvent() {
        String eventId = askId("Event id to delete");
        service.deleteEvent(eventId);
        view.showMessage(eventId + " is deleted.");
    }

    // ---- Attendees ----

    /**
     * Everything is checked (event, add-ons, contact method, name) before a
     * new person is put on file, and they are stored only once the
     * registration has gone through, so a refused registration leaves no
     * stray attendee behind.
     */
    private void registerAttendee() {
        Event event = requireEvent(askId("Event id"));
        if (event.isCancelled()) {
            throw new IllegalStateException(event.getName() + " is cancelled");
        }
        if (!event.getDateTime().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException(event.getName() + " has already started - registration is closed");
        }
        view.showList(attendeeLines(), "No attendees on file yet.");
        String attendeeId = askId("Attendee id (leave empty for a new person)");
        Attendee attendee;
        boolean isNew = attendeeId.isEmpty();
        TicketAddOn[] addOns;
        if (isNew) {
            String name = view.ask("Name");
            String email = optional(view.ask("E-mail"));
            String phone = optional(view.ask("Phone"));
            ContactPreference contact = parseContact(view.ask("Contact by (email / sms / social) [email]"),
                    ContactPreference.EMAIL);
            addOns = parseAddOns(view.ask("Add-ons, separated by commas (vip, meal, parking) or empty"));
            attendee = new Attendee(name, email, phone, contact);
        } else {
            attendee = requireAttendee(attendeeId);
            addOns = parseAddOns(view.ask("Add-ons, separated by commas (vip, meal, parking) or empty"));
        }
        RegistrationResult result = service.register(event.getId(), attendee, LocalDateTime.now(), addOns);
        if (isNew) {
            service.addAttendee(attendee);
        }
        switch (result.status()) {
            case REGISTERED:
                view.showMessage("Registered " + attendee + ": " + result.ticket());
                break;
            case WAITLISTED:
                view.showMessage("The event is full; " + attendee + " is number "
                        + result.waitlistPosition() + " on the waitlist.");
                break;
            default:
                view.showMessage(attendee + " is " + result.status().name().toLowerCase(Locale.ROOT).replace('_', ' ') + ".");
        }
        if (isNew) {
            view.showMessage("New attendee id: " + attendee.getId() + " (use it to check in).");
        }
    }

    private void cancelRegistration() {
        Event event = requireEvent(askId("Event id"));
        Attendee attendee = requireAttendee(askId("Attendee id (e.g. ATT-1)"));
        if (service.cancelRegistration(event.getId(), attendee)) {
            view.showMessage(attendee + " is no longer registered or waitlisted for " + event.getName() + ".");
        } else {
            view.showError(attendee + " was neither registered nor waitlisted for " + event.getName() + ".");
        }
    }

    private void checkIn() {
        String eventId = askId("Event id");
        Attendee attendee = requireAttendee(askId("Attendee id (e.g. ATT-1)"));
        service.checkIn(eventId, attendee);
        view.showMessage(attendee + " checked in.");
    }

    /** Empty answers keep the current value; "-" clears the e-mail or phone. */
    private void updateAttendee() {
        view.showList(attendeeLines(), "No attendees on file yet.");
        Attendee attendee = requireAttendee(askId("Attendee id (e.g. ATT-1)"));
        String name = keep(view.ask("Name [" + attendee.getName() + "]"), attendee.getName());
        String email = keepOrClear(view.ask("E-mail [" + shown(attendee.getEmail()) + "] (- to clear)"),
                attendee.getEmail());
        String phone = keepOrClear(view.ask("Phone [" + shown(attendee.getPhone()) + "] (- to clear)"),
                attendee.getPhone());
        ContactPreference preference = parseContact(
                view.ask("Contact by (email / sms / social) [" + attendee.getPreferredContact() + "]"),
                attendee.getPreferredContact());
        attendee.updateContact(name, email, phone, preference);
        view.showMessage("Updated " + attendee + ".");
    }

    private List<String> attendeeLines() {
        List<String> lines = new ArrayList<>();
        for (Attendee attendee : service.listAttendees()) {
            lines.add(attendee.toString());
        }
        return lines;
    }

    private static List<String> summaries(List<Event> events) {
        List<String> lines = new ArrayList<>();
        for (Event event : events) {
            lines.add(event.summary());
        }
        return lines;
    }

    // ---- Looking records up by id ----

    private Event requireEvent(String id) {
        return service.findEvent(id).orElseThrow(() -> new IllegalArgumentException("No event with id " + id));
    }

    private Attendee requireAttendee(String id) {
        Attendee attendee = service.findAttendee(id);
        if (attendee == null) {
            throw new IllegalArgumentException("No attendee with id " + id);
        }
        return attendee;
    }

    /** Asks for an ID and normalizes it, so "evt-1" finds EVT-1. */
    private String askId(String prompt) {
        return view.ask(prompt).trim().toUpperCase(Locale.ROOT);
    }

    // ---- Turning what the user typed into values ----

    /** An empty answer means "not given". */
    private static String optional(String text) {
        return text.isEmpty() ? null : text;
    }

    private static String keep(String answer, String current) {
        return answer.isEmpty() ? current : answer;
    }

    private static String keepOrClear(String answer, String current) {
        if (answer.equals("-")) {
            return null;
        }
        return answer.isEmpty() ? current : answer;
    }

    private static String shown(String value) {
        return value == null ? "none" : value;
    }

    private static int parseInt(String text, String what) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(what + " must be a whole number");
        }
    }

    private static double parseDouble(String text, String what) {
        try {
            double value = Double.parseDouble(text.trim());
            if (!Double.isFinite(value)) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(what + " must be a number, e.g. 250");
        }
    }

    private static LocalDateTime parseDateTime(String text) {
        try {
            return LocalDateTime.parse(text.trim(), DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date and time must look like 2026-12-01 18:00");
        }
    }

    /** @param fallback used for an empty answer */
    private static ContactPreference parseContact(String text, ContactPreference fallback) {
        String key = text.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            return fallback;
        }
        if (key.equals("email") || key.equals("e-mail")) {
            return ContactPreference.EMAIL;
        }
        if (key.equals("sms")) {
            return ContactPreference.SMS;
        }
        if (key.equals("social") || key.equals("social media") || key.equals("social_media")) {
            return ContactPreference.SOCIAL_MEDIA;
        }
        throw new IllegalArgumentException("Unknown contact method: " + text);
    }

    /** "vip, meal" becomes [VIP, MEAL] (each a DECORATOR on the ticket); empty parts are skipped. */
    private static TicketAddOn[] parseAddOns(String text) {
        List<TicketAddOn> chosen = new ArrayList<>();
        for (String part : text.split(",")) {
            if (!part.isBlank()) {
                chosen.add(TicketAddOn.fromString(part.trim()));
            }
        }
        return chosen.toArray(new TicketAddOn[0]);
    }
}
