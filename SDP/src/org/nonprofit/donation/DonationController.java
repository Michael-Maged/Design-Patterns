package org.nonprofit.donation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.EventBus;
import org.nonprofit.core.SystemEventType;

/**
 * MVC pattern (CONTROLLER): reads the menu choice from the view, calls the
 * model (DonationService) and tells the view what to show.
 * The view never talks to the model, and the model knows nothing about menus.
 */
public class DonationController {
    private final DonationService service;
    private final DonationView view;

    public DonationController(DonationService service, DonationView view) {
        this.service = service;
        this.view = view;
        // The view wants to hear about new donations (Observer).
        EventBus.getInstance().subscribe(SystemEventType.DONATION_RECEIVED, view);
    }

    public void run() {
        boolean running = true;
        while (running) {
            int choice = view.showMenu();
            try {
                switch (choice) {
                    case 1: view.showList("Donors", donorSummaries()); break;
                    case 2: addDonor(); break;
                    case 3: removeDonor(); break;
                    case 4: addCampaign(); break;
                    case 5: view.showList("Campaigns", service.getCampaigns()); break;
                    case 6: makeDonation(); break;
                    case 7: view.showList("Donations", service.getDonations()); break;
                    case 8: view.showMessage(service.makeReceipt(view.ask("Donation id (e.g. DON-1)"), LocalDate.now())); break;
                    case 9: taxStatement(); break;
                    case 10: summary(); break;
                    case 0: running = false; break;
                    default: view.showError("Please choose a number from the menu.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                // Wrong input should show a message, not crash the program.
                view.showError(e.getMessage());
            }
        }
        view.showMessage("Goodbye.");
    }

    private void addDonor() {
        String name = view.ask("Name");
        String email = view.ask("E-mail");
        String phone = view.ask("Phone");
        ContactPreference contact = parseContact(view.ask("Contact by (email / sms / social) [email]"));
        Donor donor = service.addDonor(name, email, phone, contact);
        view.showMessage("Added " + donor.summary());
    }

    private void removeDonor() {
        String id = view.ask("Donor id to remove");
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
        String donorId = view.ask("Donor id (e.g. DNR-1)");
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

        Donation donation = service.donate(donorId, type, LocalDate.now(), form);
        view.showMessage("Recorded " + donation);
    }

    private void taxStatement() {
        String donorId = view.ask("Donor id");
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
