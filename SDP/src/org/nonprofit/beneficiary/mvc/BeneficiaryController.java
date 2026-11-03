package org.nonprofit.beneficiary.mvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.beneficiary.strategy.AllocationMode;
import org.nonprofit.beneficiary.model.Beneficiary;
import org.nonprofit.beneficiary.model.BeneficiaryService;
import org.nonprofit.beneficiary.model.Distribution;
import org.nonprofit.beneficiary.model.Need;
import org.nonprofit.beneficiary.model.Urgency;

/**
 * MVC pattern (controller): takes the user's menu choice from the
 * {@link BeneficiaryView}, calls the model ({@link BeneficiaryService} and the
 * beneficiaries, needs and distributions it manages) and tells the view what
 * to show. The view never talks to the model directly, and the model knows
 * nothing about menus.
 *
 * After every action the view shows the messages the Communication module
 * sent (passed in as {@code deliveries}, so this module does not import
 * Communication). IDs can be typed in any case ("ben-1" finds BEN-1).
 */
public class BeneficiaryController {
    private final BeneficiaryService service;
    private final BeneficiaryView view;
    /** True when opened from the main menu: option 0 goes "Back" instead of ending the program. */
    private final boolean embedded;
    /** Gives the delivery lines recorded since the last call (none when nothing sends messages). */
    private final Supplier<List<String>> deliveries;

    /**
     * @param embedded   true when another menu opens this screen, so 0 means "Back"
     * @param deliveries where to take the messages sent since the last action
     *                   (e.g. {@code CommunicationService::takeOutput})
     */
    public BeneficiaryController(BeneficiaryService service, BeneficiaryView view, boolean embedded,
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
            int choice = view.showMenu(service.getStrategy().name(), embedded ? "Back" : "Exit");
            try {
                switch (choice) {
                    case 1: view.showList(lines(service.listBeneficiaries(), Beneficiary::summary), "No beneficiaries yet."); break;
                    case 2: registerBeneficiary(); break;
                    case 3: updateBeneficiary(); break;
                    case 4: removeBeneficiary(); break;
                    case 5: addNeed(); break;
                    case 6: updateNeed(); break;
                    case 7: removeNeed(); break;
                    case 8: view.showList(lines(service.openNeeds(), Need::summary), "No open needs."); break;
                    case 9: changeAllocationRule(); break;
                    case 10: previewDistribution(); break;
                    case 11: distribute(); break;
                    case 12: giveDirectly(); break;
                    case 13: view.showList(lines(service.listDistributions(), Distribution::summary), "No distributions yet."); break;
                    case 14: impactReport(); break;
                    case 0: running = false; break;
                    default: view.showError("Please choose a number from the menu.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                // Rules broken in the model (need already partly met, ...) are shown, not thrown.
                view.showError(e.getMessage());
            }
            view.showDeliveries(deliveries.get());
        }
        if (!embedded) {
            view.showMessage("Goodbye.");
        }
    }

    // ---- Beneficiaries ----

    private void registerBeneficiary() {
        String name = view.ask("Name");
        String email = optional(view.ask("E-mail (leave empty if none)"));
        String phone = optional(view.ask("Phone"));
        ContactPreference contact = parseContact(view.ask("Contact by (email / sms / social) [sms]"),
                ContactPreference.SMS);
        String area = view.ask("Area");
        int household = parseInt(view.ask("Household size"), "Household size");
        Beneficiary beneficiary = service.registerBeneficiary(
                new Beneficiary(name, email, phone, contact, area, household));
        view.showMessage("Registered " + beneficiary.summary());
    }

    /** Empty answers keep the current value; "-" clears the e-mail or phone. */
    private void updateBeneficiary() {
        Beneficiary b = requireBeneficiary(askId("Beneficiary id (e.g. BEN-1)"));
        String name = keep(view.ask("Name [" + b.getName() + "]"), b.getName());
        String email = keepOrClear(view.ask("E-mail [" + shown(b.getEmail()) + "] (- to clear)"), b.getEmail());
        String phone = keepOrClear(view.ask("Phone [" + shown(b.getPhone()) + "] (- to clear)"), b.getPhone());
        ContactPreference preference = parseContact(
                view.ask("Contact by (email / sms / social) [" + b.getPreferredContact() + "]"),
                b.getPreferredContact());
        String area = keep(view.ask("Area [" + b.getArea() + "]"), b.getArea());
        String householdText = view.ask("Household size [" + b.getHouseholdSize() + "]");
        int household = householdText.isEmpty() ? b.getHouseholdSize() : parseInt(householdText, "Household size");
        Beneficiary updated = service.updateBeneficiary(b.getId(), name, email, phone, preference, area, household);
        view.showMessage("Updated " + updated.summary());
    }

    private void removeBeneficiary() {
        String id = askId("Beneficiary id to remove");
        service.removeBeneficiary(id);
        view.showMessage("Removed " + id + " and their open needs.");
    }

    // ---- Needs ----

    private void addNeed() {
        String beneficiaryId = requireBeneficiary(askId("Beneficiary id (e.g. BEN-1)")).getId();
        String category = view.ask("Category (e.g. food parcel)");
        int quantity = parseInt(view.ask("Quantity"), "Quantity");
        Urgency urgency = Urgency.fromString(view.ask("Urgency (low / medium / high)"));
        Need need = service.addNeed(beneficiaryId, category, quantity, urgency, LocalDate.now());
        view.showMessage("Added " + need.summary());
    }

    private void updateNeed() {
        Need need = requireNeed(askId("Need id (e.g. NED-1)"));
        String quantityText = view.ask("Quantity [" + need.getQuantity() + "]");
        int quantity = quantityText.isEmpty() ? need.getQuantity() : parseInt(quantityText, "Quantity");
        String urgencyText = view.ask("Urgency (low / medium / high) [" + need.getUrgency() + "]");
        Urgency urgency = urgencyText.isEmpty() ? need.getUrgency() : Urgency.fromString(urgencyText);
        view.showMessage("Updated " + service.updateNeed(need.getId(), quantity, urgency).summary());
    }

    private void removeNeed() {
        String id = askId("Need id to remove");
        service.removeNeed(id);
        view.showMessage("Removed " + id + ".");
    }

    // ---- Allocation and distribution ----

    private void changeAllocationRule() {
        service.setStrategy(AllocationMode.fromString(view.ask("Rule (urgency / first come / equal)")));
        view.showMessage("Allocation rule is now: " + service.getStrategy().name());
    }

    private void previewDistribution() {
        String category = view.ask("Category");
        int units = parseInt(view.ask("Units in stock"), "Units");
        Map<Need, Integer> shares = service.preview(category, units);
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
        view.showMessage("Preview only (" + service.getStrategy().name() + "): " + total + " of " + units
                + " unit(s) would be handed out; nothing was recorded.");
    }

    private void distribute() {
        String program = view.ask("Program name");
        String category = view.ask("Category");
        int units = parseInt(view.ask("Units in stock"), "Units");
        List<Distribution> made = service.distribute(program, category, units, LocalDate.now());
        if (made.isEmpty()) {
            view.showMessage("Nobody has an open need for " + category + "; nothing was handed out.");
            return;
        }
        view.showList(lines(made, Distribution::summary), "");
        view.showMessage(made.size() + " distribution(s) recorded.");
    }

    private void giveDirectly() {
        view.showList(lines(service.openNeeds(), Need::summary), "No open needs.");
        String needId = askId("Need id (e.g. NED-4)");
        int units = parseInt(view.ask("Units to give"), "Units");
        String program = view.ask("Program name");
        Distribution d = service.giveDirectly(needId, units, program, LocalDate.now());
        view.showMessage("Recorded " + d.summary());
    }

    private void impactReport() {
        view.showMessage("Programs so far: " + service.listPrograms());
        String target = view.ask("Program name, or beneficiary id (e.g. BEN-1)");
        String asId = normalizeId(target);
        if (asId.startsWith("BEN-")) {
            view.showText(service.progressReport(asId, LocalDate.now()));
        } else {
            view.showText(service.programImpactReport(target, LocalDate.now()));
        }
    }

    // ---- Looking records up by id ----

    private Beneficiary requireBeneficiary(String id) {
        Beneficiary beneficiary = service.findBeneficiary(id);
        if (beneficiary == null) {
            throw new IllegalArgumentException("No beneficiary with id " + id);
        }
        return beneficiary;
    }

    private Need requireNeed(String id) {
        Need need = service.findNeed(id);
        if (need == null) {
            throw new IllegalArgumentException("No need with id " + id);
        }
        return need;
    }

    /** Asks for an ID and normalizes it, so "ben-1" finds BEN-1. */
    private String askId(String prompt) {
        return normalizeId(view.ask(prompt));
    }

    /** One line per record, e.g. {@code lines(needs, Need::summary)}. */
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

    private static int parseInt(String text, String what) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(what + " must be a whole number");
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
}
