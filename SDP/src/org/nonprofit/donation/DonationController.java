package org.nonprofit.donation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.EventBus;
import org.nonprofit.core.SystemEventType;

/**
 * MVC pattern (CONTROLLER): reads the menu choice from the view, calls the
 * model (DonationService) and tells the view what to show.
 * The view never talks to the model, and the model knows nothing about menus.
 *
 * When the main menu opens this screen it says so (embedded), and passes where
 * to read the messages the Communication module sent (the thank-you e-mails),
 * so the view can show them after each action. This module does not import Communication.
 */
public class DonationController {
    private final DonationService service;
    private final DonationView view;
    /** True when opened from the main menu: option 0 goes "Back" instead of ending the program. */
    private final boolean embedded;
    /** Gives the messages sent since the last call (none when nothing sends messages). */
    private final Supplier<List<String>> deliveries;

    public DonationController(DonationService service, DonationView view) {
        this(service, view, false, List::of);
    }

    /**
     * @param embedded   true when another menu opens this screen, so 0 means "Back"
     * @param deliveries where to take the messages sent since the last action
     *                   (e.g. CommunicationService::takeOutput)
     */
    public DonationController(DonationService service, DonationView view, boolean embedded,
            Supplier<List<String>> deliveries) {
        this.service = service;
        this.view = view;
        this.embedded = embedded;
        this.deliveries = deliveries;
        // The view wants to hear about new donations (Observer).
        EventBus.getInstance().subscribe(SystemEventType.DONATION_RECEIVED, view);
    }

    public void run() {
        boolean running = true;
        while (running) {
            int choice = view.showMenu(embedded ? "Back" : "Exit");
            try {
                switch (choice) {
                    case 1: view.showList("Donors", donorSummaries()); break;
                    case 2: addDonor(); break;
                    case 3: removeDonor(); break;
                    case 4: addCampaign(); break;
                    case 5: view.showList("Campaigns", service.getCampaigns()); break;
                    case 6: makeDonation(); break;
                    case 7: view.showList("Donations", service.getDonations()); break;
                    case 8: view.showMessage(service.makeReceipt(askId("Donation id (e.g. DON-1)"), LocalDate.now())); break;
                    case 9: taxStatement(); break;
                    case 10: summary(); break;
                    case 11: updateDonor(); break;
                    case 0: running = false; break;
                    default: view.showError("Please choose a number from the menu.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                // Wrong input should show a message, not crash the program.
                view.showError(e.getMessage());
            }
            view.showDeliveries(deliveries.get());
        }
        if (!embedded) {
            view.showMessage("Goodbye.");
        }
    }

    private void addDonor() {
        String name = view.ask("Name");
        String email = view.ask("E-mail");
        String phone = view.ask("Phone");
        ContactPreference contact = parseContact(view.ask("Contact by (email / sms / social) [email]"));
        Donor donor = service.addDonor(name, email, phone, contact);
        view.showMessage("Added " + donor.summary());
    }

    /** Shows the current values in brackets; an empty answer keeps the value. */
    private void updateDonor() {
        String donorId = askId("Donor id (e.g. DNR-1)");
        Donor donor = service.findDonor(donorId);
        if (donor == null) {
            throw new IllegalArgumentException("No donor with id " + donorId);
        }
        String name = keep(view.ask("Name [" + donor.getName() + "]"), donor.getName());
        String email = keep(view.ask("E-mail [" + donor.getEmail() + "]"), donor.getEmail());
        String phone = keep(view.ask("Phone [" + donor.getPhone() + "]"), donor.getPhone());
        String contactText = view.ask("Contact by (email / sms / social) [" + donor.getPreferredContact() + "]");
        ContactPreference contact = contactText.isEmpty() ? donor.getPreferredContact() : parseContact(contactText);
        Donor updated = service.updateDonor(donorId, name, email, phone, contact);
        view.showMessage("Updated " + updated + ": " + updated.getEmail() + ", " + updated.getPhone()
                + ", contact by " + updated.getPreferredContact());
    }

    private void removeDonor() {
        String id = askId("Donor id to remove");
        service.removeDonor(id);
        view.showMessage("Removed " + id + ".");
    }

    private void addCampaign() {
        String name = view.ask("Campaign name");
        double goal = parseAmount(view.ask("Goal amount"));
        view.showMessage("Added " + service.addCampaign(name, goal));
    }

    /** Asks only the questions that make sense for the chosen type. */
    private void makeDonation() {
        String donorId = askId("Donor id (e.g. DNR-1)");
        if (service.findDonor(donorId) == null) {
            throw new IllegalArgumentException("No donor with id " + donorId);
        }
        DonationType type = DonationType.fromText(view.ask("Type (online / check / in-kind)"));
        String amountQuestion = type == DonationType.IN_KIND ? "Estimated value of the goods" : "Amount";
        DonationForm form = new DonationForm(parseAmount(view.ask(amountQuestion)));

        switch (type) {
            case ONLINE:
                form.setReference(view.ask("Payment reference"));
                form.setMonthly(view.ask("Repeat every month? (y/n)").equalsIgnoreCase("y"));
                break;
            case CHECK:
                form.setReference(view.ask("Check number"));
                form.setBank(view.ask("Bank name"));
                break;
            case IN_KIND:
                form.setItem(view.ask("What items were given"));
                break;
            default:
                break;
        }
        form.setCampaign(view.ask("Campaign (leave empty if none)"));
        LocalDate date = parseDate(view.ask("Date received (yyyy-mm-dd) [today]"));

        // The service refuses a date after today (a backdated check is fine).
        Donation donation = service.donate(donorId, type, date, form, LocalDate.now());
        view.showMessage("Recorded " + donation);
    }

    private void taxStatement() {
        String donorId = askId("Donor id");
        String yearText = view.ask("Year [" + LocalDate.now().getYear() + "]");
        int year = yearText.isEmpty() ? LocalDate.now().getYear() : parseYear(yearText);
        view.showMessage(service.makeTaxStatement(donorId, year, LocalDate.now()));
    }

    private void summary() {
        view.showMessage(service.summaryReport());
        view.showList("Top donors", donorSummaries(service.getTopDonors(3)));
    }

    // ---- Small helpers that turn typed text into values ----

    private List<String> donorSummaries() {
        return donorSummaries(service.getDonors());
    }

    private static List<String> donorSummaries(List<Donor> donors) {
        List<String> lines = new ArrayList<>();
        for (Donor donor : donors) {
            lines.add(donor.summary());
        }
        return lines;
    }

    /** IDs can be typed in any case: "dnr-1" finds DNR-1. */
    private String askId(String question) {
        return view.ask(question).trim().toUpperCase(Locale.ROOT);
    }

    private static String keep(String answer, String current) {
        return answer.isEmpty() ? current : answer;
    }

    /** Empty means today. */
    private static LocalDate parseDate(String text) {
        if (text.isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Please type a date like 2026-03-15, or leave it empty for today");
        }
    }

    private static double parseAmount(String text) {
        try {
            return Double.parseDouble(text.replace(",", "").replace("$", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please type a number, for example 250 or 99.50");
        }
    }

    private static int parseYear(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please type a year, for example 2026");
        }
    }

    private static ContactPreference parseContact(String text) {
        String clean = text.trim().toLowerCase(Locale.ROOT);
        if (clean.isEmpty() || clean.equals("email") || clean.equals("e-mail")) {
            return ContactPreference.EMAIL;
        }
        if (clean.equals("sms")) {
            return ContactPreference.SMS;
        }
        if (clean.equals("social") || clean.equals("social media")) {
            return ContactPreference.SOCIAL_MEDIA;
        }
        throw new IllegalArgumentException("Unknown contact method: " + text + " (use email, sms or social)");
    }
}
