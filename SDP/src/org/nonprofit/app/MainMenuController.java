package org.nonprofit.app;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.nonprofit.beneficiary.AllocationMode;
import org.nonprofit.beneficiary.Beneficiary;
import org.nonprofit.beneficiary.BeneficiaryService;
import org.nonprofit.beneficiary.Distribution;
import org.nonprofit.beneficiary.Need;
import org.nonprofit.beneficiary.Urgency;
import org.nonprofit.communication.CommunicationService;
import org.nonprofit.communication.SentMessage;
import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.Person;
import org.nonprofit.donation.DonationController;
import org.nonprofit.event.Attendee;
import org.nonprofit.event.Event;
import org.nonprofit.event.EventService;
import org.nonprofit.event.EventType;
import org.nonprofit.event.RegistrationResult;
import org.nonprofit.event.TicketAddOn;
import org.nonprofit.volunteer.VolunteerController;

/**
 * MVC pattern (controller): runs the main console menu. It takes the
 * user's choice from the {@link MainMenuView}, calls the module services
 * (the model) that the {@link DataStore} Singleton owns, and tells the view
 * what to show. Rule violations and bad input are shown as {@code !!}
 * messages and the menu comes back; nothing the user types stops the program.
 *
 * After every action the view also shows what the Communication module sent
 * and any new notes in the attendees' inboxes; the model classes keep that
 * text and never print it themselves.
 *
 * IDs can be typed in any case ("ben-1" finds BEN-1). The Donations and
 * Volunteers options open each module's own MVC screen
 * ({@link DonationController}, {@link VolunteerController}), which work on the
 * shared DonationService and VolunteerService.
 */
public class MainMenuController {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DataStore store;
    private final EventService events;
    private final BeneficiaryService beneficiaries;
    private final CommunicationService communication;
    private final MainMenuView view;
    private final DonationController donationScreen;
    private final VolunteerController volunteerScreen;

    public MainMenuController(DataStore store, MainMenuView view, DonationController donationScreen,
            VolunteerController volunteerScreen) {
        if (store == null || view == null || donationScreen == null || volunteerScreen == null) {
            throw new IllegalArgumentException("Data store, view, donation screen and volunteer screen are required");
        }
        this.store = store;
        this.events = store.getEventService();
        this.beneficiaries = store.getBeneficiaryService();
        this.communication = store.getCommunicationService();
        this.view = view;
        this.donationScreen = donationScreen;
        this.volunteerScreen = volunteerScreen;
    }

    /** Shows the main menu again after every screen until the user chooses Exit. */
    public void run() {
        boolean running = true;
        while (running) {
            switch (view.showMainMenu()) {
                case 1: donationScreen.run(); break;
                case 2: volunteerScreen.run(); break;
                case 3: events(); break;
                case 4: beneficiaries(); break;
                case 5: communication(); break;
                case 0: running = false; break;
                default: view.showError("Please choose a number from the menu.");
            }
        }
        view.showMessage("Goodbye.");
    }

    /** Shows the messages sent and the attendee notes received since the last action. */
    private void showNewMessages() {
        view.showDeliveries(communication.takeOutput());
        for (Attendee attendee : store.listAttendees()) {
            view.showInbox(attendee.getName(), attendee.takeNewNotes());
        }
    }

    // ---- Events ----

    private void events() {
        boolean open = true;
        while (open) {
            int choice = view.showEventMenu();
            try {
                switch (choice) {
                    case 1: view.showList(lines(events.listEvents(), Event::summary), "No events yet."); break;
                    case 2: view.showList(lines(events.upcomingEvents(LocalDateTime.now()), Event::summary), "No upcoming events."); break;
                    case 3: createEvent(); break;
                    case 4: editEvent(); break;
                    case 5: registerAttendee(); break;
                    case 6: cancelRegistration(); break;
                    case 7: checkIn(); break;
                    case 8: updateAttendee(); break;
                    case 9: sendReminder(); break;
                    case 10: sendReminders(); break;
                    case 11: view.showText(events.attendanceReport(askId("Event id (e.g. EVT-1)"))); break;
                    case 12: cancelEvent(); break;
                    case 13: deleteEvent(); break;
                    case 0: open = false; break;
                    default: view.showError("Please choose a number from the menu.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
            showNewMessages();
        }
    }

    private void createEvent() {
        String type = view.ask("Type (fundraiser / workshop / outreach)");
        String name = view.ask("Name");
        LocalDateTime when = parseDateTime(view.ask("Date and time (yyyy-MM-dd HH:mm)"));
        String location = view.ask("Location");
        int capacity = parseInt(view.ask("Capacity"), "Capacity");
        Event event = events.createEvent(type, name, when, location, capacity);
        view.showMessage("Created " + event.summary());
    }

    private void editEvent() {
        Event event = requireEvent(askId("Event id (e.g. EVT-1)"));
        String id = event.getId();
        switch (view.showEventEditMenu(event.toString())) {
            case 1: events.renameEvent(id, view.ask("New name")); break;
            case 2: events.reschedule(id, parseDateTime(view.ask("New date and time (yyyy-MM-dd HH:mm)"))); break;
            case 3: events.relocate(id, view.ask("New location")); break;
            case 4: events.setCapacity(id, parseInt(view.ask("New capacity"), "Capacity")); break;
            case 5:
                events.setBaseTicketPrice(id, parseDouble(view.ask("New base ticket price"), "Price"));
                view.showMessage(String.format("New tickets for %s now start at $%.2f.",
                        event.getName(), event.getBaseTicketPrice()));
                return;
            case 6:
                requireType(event, EventType.FUNDRAISER);
                events.setFundraiserGoal(id, parseDouble(view.ask("Fundraising goal"), "Goal"));
                break;
            case 7:
                requireType(event, EventType.FUNDRAISER);
                events.recordPledge(id, parseDouble(view.ask("Pledge amount"), "Pledge"));
                break;
            case 8:
                requireType(event, EventType.WORKSHOP);
                events.setWorkshopDetails(id, view.ask("Topic"), view.ask("Instructor"));
                break;
            case 9:
                requireType(event, EventType.OUTREACH);
                events.setTargetCommunity(id, view.ask("Target community"));
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
        RegistrationResult result = events.register(event.getId(), attendee, LocalDateTime.now(), addOns);
        if (isNew) {
            store.addAttendee(attendee);
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
        if (events.cancelRegistration(event.getId(), attendee)) {
            view.showMessage(attendee + " is no longer registered or waitlisted for " + event.getName() + ".");
        } else {
            view.showError(attendee + " was neither registered nor waitlisted for " + event.getName() + ".");
        }
    }

    private void checkIn() {
        String eventId = askId("Event id");
        Attendee attendee = requireAttendee(askId("Attendee id (e.g. ATT-1)"));
        events.checkIn(eventId, attendee);
        view.showMessage(attendee + " checked in.");
    }

    private void updateAttendee() {
        view.showList(attendeeLines(), "No attendees on file yet.");
        Attendee attendee = requireAttendee(askId("Attendee id (e.g. ATT-1)"));
        String[] contact = askContactChanges(attendee);
        ContactPreference preference = parseContact(contact[3], attendee.getPreferredContact());
        attendee.updateContact(contact[0], contact[1], contact[2], preference);
        view.showMessage("Updated " + attendee + ".");
    }

    private void sendReminder() {
        Event event = requireEvent(askId("Event id"));
        events.sendReminder(event.getId(), LocalDateTime.now());
        view.showMessage("Reminder sent to the " + event.getAttendees().size() + " people registered for "
                + event.getName() + ".");
    }

    private void sendReminders() {
        List<Event> reminded = events.sendUpcomingReminders(LocalDateTime.now(), Duration.ofHours(24));
        view.showMessage(reminded.isEmpty() ? "No events in the next 24 hours."
                : "Reminders sent for " + reminded + ".");
    }

    private void cancelEvent() {
        String eventId = askId("Event id to cancel");
        events.cancelEvent(eventId);
        view.showMessage(eventId + " is cancelled.");
    }

    private void deleteEvent() {
        String eventId = askId("Event id to delete");
        events.deleteEvent(eventId);
        view.showMessage(eventId + " is deleted.");
    }

    private List<String> attendeeLines() {
        return lines(store.listAttendees(), Attendee::toString);
    }

    // ---- Beneficiaries ----

    private void beneficiaries() {
        boolean open = true;
        while (open) {
            int choice = view.showBeneficiaryMenu(beneficiaries.getStrategy().name());
            try {
                switch (choice) {
                    case 1: view.showList(lines(beneficiaries.listBeneficiaries(), Beneficiary::summary), "No beneficiaries yet."); break;
                    case 2: registerBeneficiary(); break;
                    case 3: updateBeneficiary(); break;
                    case 4: removeBeneficiary(); break;
                    case 5: addNeed(); break;
                    case 6: updateNeed(); break;
                    case 7: removeNeed(); break;
                    case 8: view.showList(lines(beneficiaries.openNeeds(), Need::summary), "No open needs."); break;
                    case 9: changeAllocationRule(); break;
                    case 10: previewDistribution(); break;
                    case 11: distribute(); break;
                    case 12: giveDirectly(); break;
                    case 13: view.showList(lines(beneficiaries.listDistributions(), Distribution::summary), "No distributions yet."); break;
                    case 14: impactReport(); break;
                    case 0: open = false; break;
                    default: view.showError("Please choose a number from the menu.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
            showNewMessages();
        }
    }

    private void registerBeneficiary() {
        String name = view.ask("Name");
        String email = optional(view.ask("E-mail (leave empty if none)"));
        String phone = optional(view.ask("Phone"));
        ContactPreference contact = parseContact(view.ask("Contact by (email / sms / social) [sms]"),
                ContactPreference.SMS);
        String area = view.ask("Area");
        int household = parseInt(view.ask("Household size"), "Household size");
        Beneficiary beneficiary = beneficiaries.registerBeneficiary(
                new Beneficiary(name, email, phone, contact, area, household));
        view.showMessage("Registered " + beneficiary.summary());
    }

    private void updateBeneficiary() {
        Beneficiary b = requireBeneficiary(askId("Beneficiary id (e.g. BEN-1)"));
        String[] contact = askContactChanges(b);
        ContactPreference preference = parseContact(contact[3], b.getPreferredContact());
        String area = keep(view.ask("Area [" + b.getArea() + "]"), b.getArea());
        String householdText = view.ask("Household size [" + b.getHouseholdSize() + "]");
        int household = householdText.isEmpty() ? b.getHouseholdSize() : parseInt(householdText, "Household size");
        Beneficiary updated = beneficiaries.updateBeneficiary(b.getId(), contact[0], contact[1], contact[2],
                preference, area, household);
        view.showMessage("Updated " + updated.summary());
    }

    private void removeBeneficiary() {
        String id = askId("Beneficiary id to remove");
        beneficiaries.removeBeneficiary(id);
        view.showMessage("Removed " + id + " and their open needs.");
    }

    private void addNeed() {
        String beneficiaryId = requireBeneficiary(askId("Beneficiary id (e.g. BEN-1)")).getId();
        String category = view.ask("Category (e.g. food parcel)");
        int quantity = parseInt(view.ask("Quantity"), "Quantity");
        Urgency urgency = Urgency.fromString(view.ask("Urgency (low / medium / high)"));
        Need need = beneficiaries.addNeed(beneficiaryId, category, quantity, urgency, LocalDate.now());
        view.showMessage("Added " + need.summary());
    }

    private void updateNeed() {
        Need need = requireNeed(askId("Need id (e.g. NED-1)"));
        String quantityText = view.ask("Quantity [" + need.getQuantity() + "]");
        int quantity = quantityText.isEmpty() ? need.getQuantity() : parseInt(quantityText, "Quantity");
        String urgencyText = view.ask("Urgency (low / medium / high) [" + need.getUrgency() + "]");
        Urgency urgency = urgencyText.isEmpty() ? need.getUrgency() : Urgency.fromString(urgencyText);
        view.showMessage("Updated " + beneficiaries.updateNeed(need.getId(), quantity, urgency).summary());
    }

    private void removeNeed() {
        String id = askId("Need id to remove");
        beneficiaries.removeNeed(id);
        view.showMessage("Removed " + id + ".");
    }

    private void changeAllocationRule() {
        beneficiaries.setStrategy(AllocationMode.fromString(view.ask("Rule (urgency / first come / equal)")));
        view.showMessage("Allocation rule is now: " + beneficiaries.getStrategy().name());
    }

    private void previewDistribution() {
        String category = view.ask("Category");
        int units = parseInt(view.ask("Units in stock"), "Units");
        Map<Need, Integer> shares = beneficiaries.preview(category, units);
        if (shares.isEmpty()) {
            view.showMessage("Nobody has an open need for " + category + ".");
            return;
        }
        List<String> lines = new ArrayList<>();
        int total = 0;
        for (Map.Entry<Need, Integer> share : shares.entrySet()) {
            Need need = share.getKey();
            lines.add(String.format("%s %-16s would get %d of the %d still needed",
                    need.getId(), need.getBeneficiary().getName(), share.getValue(), need.remaining()));
            total += share.getValue();
        }
        view.showList(lines, "");
        view.showMessage("Preview only (" + beneficiaries.getStrategy().name() + "): " + total + " of " + units
                + " unit(s) would be handed out; nothing was recorded.");
    }

    private void distribute() {
        String program = view.ask("Program name");
        String category = view.ask("Category");
        int units = parseInt(view.ask("Units in stock"), "Units");
        List<Distribution> made = beneficiaries.distribute(program, category, units, LocalDate.now());
        if (made.isEmpty()) {
            view.showMessage("Nobody has an open need for " + category + "; nothing was handed out.");
            return;
        }
        view.showList(lines(made, Distribution::summary), "");
        view.showMessage(made.size() + " distribution(s) recorded.");
    }

    private void giveDirectly() {
        view.showList(lines(beneficiaries.openNeeds(), Need::summary), "No open needs.");
        String needId = askId("Need id (e.g. NED-4)");
        int units = parseInt(view.ask("Units to give"), "Units");
        String program = view.ask("Program name");
        Distribution d = beneficiaries.giveDirectly(needId, units, program, LocalDate.now());
        view.showMessage("Recorded " + d.summary());
    }

    private void impactReport() {
        view.showMessage("Programs so far: " + beneficiaries.listPrograms());
        String target = view.ask("Program name, or beneficiary id (e.g. BEN-1)");
        String asId = normalizeId(target);
        if (asId.startsWith("BEN-")) {
            view.showText(beneficiaries.progressReport(asId, LocalDate.now()));
        } else {
            view.showText(beneficiaries.programImpactReport(target, LocalDate.now()));
        }
    }

    // ---- Communication ----

    private void communication() {
        boolean open = true;
        while (open) {
            int choice = view.showCommunicationMenu();
            try {
                switch (choice) {
                    case 1: broadcast(); break;
                    case 2: showLog(); break;
                    case 0: open = false; break;
                    default: view.showError("Please choose a number from the menu.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
            showNewMessages();
        }
    }

    private void broadcast() {
        BroadcastGroup group = BroadcastGroup.fromString(view.ask("Group (donors / volunteers / beneficiaries / attendees)"));
        String subject = requireText(view.ask("Subject"), "Subject");
        String text = requireText(view.ask("Message"), "Message text");
        boolean urgent = parseYesNo(view.ask("Urgent? (y/n) [n]"));
        String channel = view.ask("Channel for everyone (email / sms / social), empty = each person's preference");
        ContactPreference forced = channel.isEmpty() ? null : parseContact(channel, null);
        int reached = communication.broadcast(store.membersOf(group), subject, text, urgent, forced);
        view.showMessage("Reached " + reached + " " + group.name().toLowerCase(Locale.ROOT) + ".");
    }

    private void showLog() {
        view.showList(lines(communication.getLog(), SentMessage::toString), "No messages sent yet.");
    }

    // ---- Looking records up by id ----

    private Event requireEvent(String id) {
        return events.findEvent(id).orElseThrow(() -> new IllegalArgumentException("No event with id " + id));
    }

    private Attendee requireAttendee(String id) {
        Attendee attendee = store.findAttendee(id);
        if (attendee == null) {
            throw new IllegalArgumentException("No attendee with id " + id);
        }
        return attendee;
    }

    private Beneficiary requireBeneficiary(String id) {
        Beneficiary beneficiary = beneficiaries.findBeneficiary(id);
        if (beneficiary == null) {
            throw new IllegalArgumentException("No beneficiary with id " + id);
        }
        return beneficiary;
    }

    private Need requireNeed(String id) {
        Need need = beneficiaries.findNeed(id);
        if (need == null) {
            throw new IllegalArgumentException("No need with id " + id);
        }
        return need;
    }

    // ---- Asking for values ----

    /** Asks for an ID and normalizes it, so "ben-1" finds BEN-1. */
    private String askId(String prompt) {
        return normalizeId(view.ask(prompt));
    }

    /**
     * Asks for new contact details, showing the current ones; an empty answer
     * keeps the current value and "-" clears an e-mail or phone.
     *
     * @return name, e-mail, phone and the typed contact method ("" = keep)
     */
    private String[] askContactChanges(Person person) {
        String name = keep(view.ask("Name [" + person.getName() + "]"), person.getName());
        String email = keepOrClear(view.ask("E-mail [" + shown(person.getEmail()) + "] (- to clear)"), person.getEmail());
        String phone = keepOrClear(view.ask("Phone [" + shown(person.getPhone()) + "] (- to clear)"), person.getPhone());
        String contact = view.ask("Contact by (email / sms / social) [" + person.getPreferredContact() + "]");
        return new String[] {name, email, phone, contact};
    }

    // ---- Turning objects into lines ----

    /** One line per record, e.g. {@code lines(events, Event::summary)}. */
    private static <T> List<String> lines(List<T> records, Function<T, String> toLine) {
        List<String> lines = new ArrayList<>();
        for (T record : records) {
            lines.add(toLine.apply(record));
        }
        return lines;
    }

    // ---- Turning what the user typed into values ----

    private static String normalizeId(String text) {
        return text.trim().toUpperCase(Locale.ROOT);
    }

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

    private static String requireText(String text, String what) {
        if (text.isBlank()) {
            throw new IllegalArgumentException(what + " is required");
        }
        return text;
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

    /** @param fallback used for an empty answer (null = an answer is required) */
    private static ContactPreference parseContact(String text, ContactPreference fallback) {
        String key = text.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty() && fallback != null) {
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
