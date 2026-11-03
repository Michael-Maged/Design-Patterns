package org.nonprofit.communication;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.nonprofit.core.Audience;
import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.Person;

/**
 * MVC pattern (controller): takes the user's menu choice from the
 * {@link CommunicationView}, calls the model ({@link CommunicationService})
 * and tells the view what to show. The view never talks to the model
 * directly, and the model knows nothing about menus.
 *
 * The people to write to live in the other modules, which this module does
 * not import. Whoever builds the screen (the app) passes three look-ups that
 * only hand out core types: the members of a group, any person by ID, and an
 * event (an {@link Audience}) by ID. IDs can be typed in any case ("att-1" finds ATT-1).
 */
public class CommunicationController {
    private final CommunicationService service;
    private final CommunicationView view;
    /** True when opened from the main menu: option 0 goes "Back" instead of ending the program. */
    private final boolean embedded;
    /** Group name typed by the user ("donors") to the people in it; refuses an unknown group. */
    private final Function<String, List<? extends Person>> groupMembers;
    /** Any person ID (DNR-, VOL-, ATT-, BEN-) to that person, or null. */
    private final Function<String, Person> findPerson;
    /** Event ID (EVT-) to that event's registered attendees, or null. */
    private final Function<String, Audience> eventAudience;

    /**
     * @param embedded      true when another menu opens this screen, so 0 means "Back"
     * @param groupMembers  the people in a group, by the name the user typed
     * @param findPerson    any person by ID, or null if there is none
     * @param eventAudience an event by ID, or null if there is none
     */
    public CommunicationController(CommunicationService service, CommunicationView view, boolean embedded,
            Function<String, List<? extends Person>> groupMembers, Function<String, Person> findPerson,
            Function<String, Audience> eventAudience) {
        this.service = service;
        this.view = view;
        this.embedded = embedded;
        this.groupMembers = groupMembers;
        this.findPerson = findPerson;
        this.eventAudience = eventAudience;
    }

    /** Shows the menu again after every action until the user chooses Exit (or Back). */
    public void run() {
        boolean running = true;
        while (running) {
            int choice = view.showMenu(embedded ? "Back" : "Exit");
            try {
                switch (choice) {
                    case 1: broadcast(); break;
                    case 2: sendToPerson(); break;
                    case 3: messageEventAttendees(); break;
                    case 4: view.showLog(service.getLog()); break;
                    case 0: running = false; break;
                    default: view.showError("Please choose a number from the menu.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
            view.showDeliveries(service.takeOutput());
        }
        if (!embedded) {
            view.showMessage("Goodbye.");
        }
    }

    private void broadcast() {
        String group = view.ask("Group (donors / volunteers / beneficiaries / attendees)");
        List<? extends Person> members = groupMembers.apply(group);
        String subject = requireText(view.ask("Subject"), "Subject");
        String text = requireText(view.ask("Message"), "Message text");
        boolean urgent = parseYesNo(view.ask("Urgent? (y/n) [n]"));
        String channel = view.ask("Channel for everyone (email / sms / social), empty = each person's preference");
        ContactPreference forced = channel.isEmpty() ? null : parseContact(channel);
        int reached = service.broadcast(members, subject, text, urgent, forced);
        view.showMessage("Reached " + reached + " " + group.toLowerCase(Locale.ROOT) + ".");
    }

    private void sendToPerson() {
        String id = askId("Person id (e.g. DNR-1, VOL-1, ATT-1, BEN-1)");
        Person person = findPerson.apply(id);
        if (person == null) {
            throw new IllegalArgumentException("No person with id " + id);
        }
        String subject = requireText(view.ask("Subject"), "Subject");
        String text = requireText(view.ask("Message"), "Message text");
        boolean urgent = parseYesNo(view.ask("Urgent? (y/n) [n]"));
        String channel = view.ask("Channel (email / sms / social), empty = their preference ("
                + person.getPreferredContact() + ")");
        ContactPreference forced = channel.isEmpty() ? null : parseContact(channel);
        if (service.send(person, subject, text, urgent, forced)) {
            view.showMessage("Sent to " + person + ".");
        } else {
            view.showError("Nothing was sent to " + person + ".");
        }
    }

    private void messageEventAttendees() {
        String id = askId("Event id (e.g. EVT-1)");
        Audience event = eventAudience.apply(id);
        if (event == null) {
            throw new IllegalArgumentException("No event with id " + id);
        }
        List<? extends Person> attendees = event.getAudience();
        if (attendees.isEmpty()) {
            view.showMessage("Nobody is registered for " + id + "; no message sent.");
            return;
        }
        String subject = requireText(view.ask("Subject"), "Subject");
        String text = requireText(view.ask("Message"), "Message text");
        boolean urgent = parseYesNo(view.ask("Urgent? (y/n) [n]"));
        String channel = view.ask("Channel for everyone (email / sms / social), empty = each person's preference");
        ContactPreference forced = channel.isEmpty() ? null : parseContact(channel);
        int reached = service.broadcast(attendees, subject, text, urgent, forced);
        view.showMessage("Reached " + reached + " of the " + attendees.size() + " attendee(s) of " + id + ".");
    }

    // ---- Turning what the user typed into values ----

    /** Asks for an ID and normalizes it, so "att-1" finds ATT-1. */
    private String askId(String prompt) {
        return view.ask(prompt).trim().toUpperCase(Locale.ROOT);
    }

    private static String requireText(String text, String what) {
        if (text.isBlank()) {
            throw new IllegalArgumentException(what + " is required");
        }
        return text;
    }

    private static boolean parseYesNo(String text) {
        String key = text.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty() || key.equals("n") || key.equals("no")) {
            return false;
        }
        if (key.equals("y") || key.equals("yes")) {
            return true;
        }
        throw new IllegalArgumentException("Please answer y or n");
    }

    private static ContactPreference parseContact(String text) {
        String key = text.trim().toLowerCase(Locale.ROOT);
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
}
