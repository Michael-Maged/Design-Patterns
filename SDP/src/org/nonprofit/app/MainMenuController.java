package org.nonprofit.app;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.nonprofit.beneficiary.Beneficiary;
import org.nonprofit.beneficiary.Distribution;
import org.nonprofit.beneficiary.Need;
import org.nonprofit.communication.SentMessage;
import org.nonprofit.core.ContactPreference;
import org.nonprofit.donation.Donation;
import org.nonprofit.donation.DonationDetails;
import org.nonprofit.donation.DonationType;
import org.nonprofit.donation.Donor;
import org.nonprofit.event.Attendee;
import org.nonprofit.event.Event;
import org.nonprofit.event.RegistrationResult;
import org.nonprofit.volunteer.VolunteerController;

/**
 * MVC pattern (controller): runs the main console menu. It takes the
 * user's choice from the {@link MainMenuView}, calls the
 * {@link NonProfitFacade} (the model side) and tells the view what to show.
 * Rule violations and bad input are shown as {@code !!} messages and the
 * menu comes back; nothing the user types stops the program.
 *
 * The Volunteers option opens the module's own MVC screen
 * ({@link VolunteerController}), which works on the shared VolunteerService.
 */
public class MainMenuController {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NonProfitFacade facade;
    private final MainMenuView view;
    private final VolunteerController volunteerScreen;

    public MainMenuController(NonProfitFacade facade, MainMenuView view, VolunteerController volunteerScreen) {
        if (facade == null || view == null || volunteerScreen == null) {
            throw new IllegalArgumentException("Facade, view and volunteer screen are required");
        }
        this.facade = facade;
        this.view = view;
        this.volunteerScreen = volunteerScreen;
    }

    /** Shows the main menu again after every screen until the user chooses Exit. */
    public void run() {
        boolean running = true;
        while (running) {
            switch (view.showMainMenu()) {
                case 1: donations(); break;
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

    // ---- Donations ----

    private void donations() {
        boolean open = true;
        while (open) {
            int choice = view.showDonationMenu();
            try {
                switch (choice) {
                    case 1: view.showList(summariesOfDonors(facade.listDonors()), "No donors yet."); break;
                    case 2: registerDonor(); break;
                    case 3: recordDonation(); break;
                    case 4: view.showList(summariesOfDonations(facade.listDonations()), "No donations yet."); break;
                    case 5: view.showText(facade.receipt(view.ask("Donation id (e.g. DNT-1)"), LocalDate.now())); break;
                    case 6: taxStatement(); break;
                    case 7: view.showText(facade.donationSummary()); break;
                    case 0: open = false; break;
                    default: view.showError("Please choose a number from the menu.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
        }
    }

    private void registerDonor() {
        String name = view.ask("Name");
        String email = optional(view.ask("E-mail"));
        String phone = optional(view.ask("Phone"));
        ContactPreference contact = parseContact(view.ask("Contact by (email / sms / social) [email]"),
                ContactPreference.EMAIL);
        Donor donor = facade.registerDonor(name, email, phone, contact);
        view.showMessage("Registered " + donor.summary());
    }

    private void recordDonation() {
        String donorId = view.ask("Donor id (e.g. DNR-1)");
        String typeText = view.ask("Type (online / check / in-kind)");
        // Parsed here only to know which questions to ask; the facade parses it again for the factory.
        DonationType type = DonationType.fromString(typeText);
        DonationDetails details = DonationDetails.of(parseDouble(view.ask(
                type == DonationType.IN_KIND ? "Estimated value" : "Amount"), "Amount"));
        switch (type) {
            case ONLINE:
                details.reference(view.ask("Payment reference"));
                if (parseYesNo(view.ask("Monthly recurring gift? (y/n) [n]"))) {
                    details.recurring();
                }
                break;
            case CHECK:
                details.reference(view.ask("Check number")).bank(view.ask("Bank"));
                break;
            default:
                details.item(view.ask("Item description"));
        }
        String campaign = optional(view.ask("Campaign (leave empty if none)"));
        if (campaign != null) {
            details.campaign(campaign);
        }
        Donation donation = facade.recordDonation(donorId, typeText, LocalDate.now(), details);
        view.showMessage("Recorded " + donation.summary());
    }

    private void taxStatement() {
        String donorId = view.ask("Donor id");
        String yearText = view.ask("Tax year [" + LocalDate.now().getYear() + "]");
        int year = yearText.isEmpty() ? LocalDate.now().getYear() : parseInt(yearText, "Year");
        view.showText(facade.taxStatement(donorId, year, LocalDate.now()));
    }

    // ---- Events ----

    private void events() {
        boolean open = true;
        while (open) {
            int choice = view.showEventMenu();
            try {
                switch (choice) {
                    case 1: view.showList(summariesOfEvents(facade.listEvents()), "No events yet."); break;
                    case 2: createEvent(); break;
                    case 3: registerAttendee(); break;
                    case 4: checkIn(); break;
                    case 5: cancelEvent(); break;
                    case 6: sendReminders(); break;
                    case 7: view.showText(facade.attendanceReport(view.ask("Event id (e.g. EVT-1)"))); break;
                    case 0: open = false; break;
                    default: view.showError("Please choose a number from the menu.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
        }
    }

    private void createEvent() {
        String type = view.ask("Type (fundraiser / workshop / outreach)");
        String name = view.ask("Name");
        LocalDateTime when = parseDateTime(view.ask("Date and time (yyyy-MM-dd HH:mm)"));
        String location = view.ask("Location");
        int capacity = parseInt(view.ask("Capacity"), "Capacity");
        Event event = facade.createEvent(type, name, when, location, capacity);
        view.showMessage("Created " + event.summary());
    }

    private void registerAttendee() {
        String eventId = view.ask("Event id");
        List<String> known = new ArrayList<>();
        for (Attendee a : facade.listAttendees()) {
            known.add(a.toString());
        }
        view.showList(known, "No attendees on file yet.");
        String attendeeId = view.ask("Attendee id (leave empty for a new person)");
        RegistrationResult result;
        if (attendeeId.isEmpty()) {
            String name = view.ask("Name");
            String email = optional(view.ask("E-mail"));
            String phone = optional(view.ask("Phone"));
            ContactPreference contact = parseContact(view.ask("Contact by (email / sms / social) [email]"),
                    ContactPreference.EMAIL);
            List<String> addOns = splitList(view.ask("Add-ons, separated by commas (vip, meal, parking) or empty"));
            result = facade.registerNewAttendee(eventId, name, email, phone, contact, addOns);
            attendeeId = name;
        } else {
            List<String> addOns = splitList(view.ask("Add-ons, separated by commas (vip, meal, parking) or empty"));
            result = facade.registerAttendee(eventId, attendeeId, addOns);
        }
        switch (result.status()) {
            case REGISTERED:
                view.showMessage("Registered " + attendeeId + ": " + result.ticket());
                break;
            case WAITLISTED:
                view.showMessage("The event is full; " + attendeeId + " is number "
                        + result.waitlistPosition() + " on the waitlist.");
                break;
            default:
                view.showMessage(attendeeId + " is " + result.status().name().toLowerCase(Locale.ROOT).replace('_', ' ') + ".");
        }
    }

    private void checkIn() {
        String eventId = view.ask("Event id");
        String attendeeId = view.ask("Attendee id (e.g. ATT-1)");
        facade.checkIn(eventId, attendeeId);
        view.showMessage(attendeeId + " checked in.");
    }

    private void cancelEvent() {
        String eventId = view.ask("Event id to cancel");
        facade.cancelEvent(eventId);
        view.showMessage(eventId + " is cancelled.");
    }

    private void sendReminders() {
        List<Event> reminded = facade.sendUpcomingReminders(LocalDateTime.now(), 24);
        view.showMessage(reminded.isEmpty() ? "No events in the next 24 hours."
                : "Reminders sent for " + reminded + ".");
    }

    // ---- Beneficiaries ----

    private void beneficiaries() {
        boolean open = true;
        while (open) {
            int choice = view.showBeneficiaryMenu(facade.allocationRule());
            try {
                switch (choice) {
                    case 1: view.showList(summariesOfBeneficiaries(facade.listBeneficiaries()), "No beneficiaries yet."); break;
                    case 2: registerBeneficiary(); break;
                    case 3: addNeed(); break;
                    case 4: view.showList(summariesOfNeeds(facade.openNeeds()), "No open needs."); break;
                    case 5: changeAllocationRule(); break;
                    case 6: distribute(); break;
                    case 7: impactReport(); break;
                    case 0: open = false; break;
                    default: view.showError("Please choose a number from the menu.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                view.showError(e.getMessage());
            }
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
        Beneficiary beneficiary = facade.registerBeneficiary(name, email, phone, contact, area, household);
        view.showMessage("Registered " + beneficiary.summary());
    }

    private void addNeed() {
        String beneficiaryId = view.ask("Beneficiary id (e.g. BEN-1)");
        String category = view.ask("Category (e.g. food parcel)");
        int quantity = parseInt(view.ask("Quantity"), "Quantity");
        String urgency = view.ask("Urgency (low / medium / high)");
        Need need = facade.addNeed(beneficiaryId, category, quantity, urgency, LocalDate.now());
        view.showMessage("Added " + need.summary());
    }

    private void changeAllocationRule() {
        String rule = facade.setAllocationRule(view.ask("Rule (urgency / first come / equal)"));
        view.showMessage("Allocation rule is now: " + rule);
    }

    private void distribute() {
        String program = view.ask("Program name");
        String category = view.ask("Category");
        int units = parseInt(view.ask("Units in stock"), "Units");
        List<Distribution> made = facade.distribute(program, category, units, LocalDate.now());
        if (made.isEmpty()) {
            view.showMessage("Nobody has an open need for " + category + "; nothing was handed out.");
            return;
        }
        List<String> lines = new ArrayList<>();
        for (Distribution d : made) {
            lines.add(d.summary());
        }
        view.showList(lines, "");
        view.showMessage(made.size() + " distribution(s) recorded.");
    }

    private void impactReport() {
        view.showMessage("Programs so far: " + facade.listPrograms());
        String target = view.ask("Program name, or beneficiary id (e.g. BEN-1)");
        if (target.toUpperCase(Locale.ROOT).startsWith("BEN-")) {
            view.showText(facade.progressReport(target, LocalDate.now()));
        } else {
            view.showText(facade.programImpactReport(target, LocalDate.now()));
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
        }
    }

    private void broadcast() {
        BroadcastGroup group = BroadcastGroup.fromString(view.ask("Group (donors / volunteers / beneficiaries / attendees)"));
        String subject = view.ask("Subject");
        String text = view.ask("Message");
        boolean urgent = parseYesNo(view.ask("Urgent? (y/n) [n]"));
        String channel = view.ask("Channel for everyone (email / sms / social), empty = each person's preference");
        ContactPreference forced = channel.isEmpty() ? null : parseContact(channel, null);
        int reached = facade.broadcast(group, subject, text, urgent, forced);
        view.showMessage("Reached " + reached + " " + group.name().toLowerCase(Locale.ROOT) + ".");
    }

    private void showLog() {
        List<String> lines = new ArrayList<>();
        for (SentMessage m : facade.messageLog()) {
            lines.add(m.toString());
        }
        view.showList(lines, "No messages sent yet.");
    }

    // ---- Turning objects into lines ----

    private static List<String> summariesOfDonors(List<Donor> donors) {
        List<String> lines = new ArrayList<>();
        for (Donor d : donors) {
            lines.add(d.summary());
        }
        return lines;
    }

    private static List<String> summariesOfDonations(List<Donation> donations) {
        List<String> lines = new ArrayList<>();
        for (Donation d : donations) {
            lines.add(d.summary());
        }
        return lines;
    }

    private static List<String> summariesOfEvents(List<Event> events) {
        List<String> lines = new ArrayList<>();
        for (Event e : events) {
            lines.add(e.summary());
        }
        return lines;
    }

    private static List<String> summariesOfBeneficiaries(List<Beneficiary> beneficiaries) {
        List<String> lines = new ArrayList<>();
        for (Beneficiary b : beneficiaries) {
            lines.add(b.summary());
        }
        return lines;
    }

    private static List<String> summariesOfNeeds(List<Need> needs) {
        List<String> lines = new ArrayList<>();
        for (Need n : needs) {
            lines.add(n.summary());
        }
        return lines;
    }

    // ---- Turning what the user typed into values ----

    /** An empty answer means "not given". */
    private static String optional(String text) {
        return text.isEmpty() ? null : text;
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
        if (key.equals("social") || key.equals("social media")) {
            return ContactPreference.SOCIAL_MEDIA;
        }
        throw new IllegalArgumentException("Unknown contact method: " + text);
    }

    /** "vip, meal" becomes ["vip", "meal"]; empty parts are skipped. */
    private static List<String> splitList(String text) {
        List<String> parts = new ArrayList<>();
        for (String part : text.split(",")) {
            if (!part.trim().isEmpty()) {
                parts.add(part.trim());
            }
        }
        return parts;
    }
}
