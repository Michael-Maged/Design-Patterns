package org.nonprofit.gui.beneficiary;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.nonprofit.beneficiary.AllocationMode;
import org.nonprofit.beneficiary.Beneficiary;
import org.nonprofit.beneficiary.BeneficiaryService;
import org.nonprofit.beneficiary.Distribution;
import org.nonprofit.beneficiary.Need;
import org.nonprofit.beneficiary.Urgency;
import org.nonprofit.core.ContactPreference;
import org.nonprofit.gui.common.Dialogs;
import org.nonprofit.gui.common.Feed;
import org.nonprofit.gui.common.Form;
import org.nonprofit.gui.common.Inputs;
import org.nonprofit.gui.common.ScreenActions;

/**
 * MVC pattern (controller) of the Beneficiaries screen in the GUI: turns
 * each click on the {@link BeneficiaryPanel} into calls on the model
 * ({@link BeneficiaryService}, unchanged from the console version) and tells
 * the panel what to show. Rules broken in the model come back as
 * exceptions and are shown as error dialogs by {@link ScreenActions}.
 *
 * The rule drop-down swaps the service's STRATEGY at runtime, so the same
 * stock is split differently (see Preview); the progress and program impact
 * reports come from the module's TEMPLATE METHOD.
 */
public class BeneficiaryGuiController {
    private final BeneficiaryService service;
    private final BeneficiaryPanel panel;
    private final Dialogs dialogs;
    private final ScreenActions actions;

    /** The last stock the user previewed or distributed, offered again and previewed live in the rule bar. */
    private String lastCategory;
    private int lastUnits;
    private String lastProgram = "";

    /**
     * @param deliveries the messages Communication sent since the last call
     *                   ({@code CommunicationService::takeOutput}); this module does not import Communication
     */
    public BeneficiaryGuiController(BeneficiaryService service, Dialogs dialogs, Feed feed,
                                    Supplier<List<String>> deliveries) {
        this.service = service;
        this.dialogs = dialogs;
        this.panel = new BeneficiaryPanel(List.of(AllocationMode.values()), mode -> mode.create().name());
        this.actions = new ScreenActions(dialogs, feed, deliveries, this::refresh);

        panel.onRegister(() -> actions.run(this::registerBeneficiary));
        panel.onEdit(() -> actions.run(this::editBeneficiary));
        panel.onRemove(() -> actions.run(this::removeBeneficiary));
        panel.onProgressReport(() -> actions.run(this::progressReport));

        panel.onAddNeed(() -> actions.run(this::addNeed));
        panel.onEditNeed(() -> actions.run(this::editNeed));
        panel.onRemoveNeed(() -> actions.run(this::removeNeed));
        panel.onGiveDirectly(() -> actions.run(this::giveDirectly));
        panel.onOpenNeedsOnly(this::refresh);

        panel.onRuleChanged(mode -> actions.run(() -> changeRule(mode)));
        panel.onPreview(() -> actions.run(this::preview));
        panel.onDistribute(() -> actions.run(this::distribute));
        panel.onImpactReport(() -> actions.run(this::impactReport));
        refresh();
    }

    public BeneficiaryPanel getPanel() {
        return panel;
    }

    /** Reloads the three tables, the rule bar and the live preview from the service. */
    public void refresh() {
        panel.showBeneficiaries(service.listBeneficiaries());
        panel.showNeeds(panel.openNeedsOnly() ? service.openNeeds() : allNeeds());
        panel.showDistributions(service.listDistributions());
        AllocationMode mode = currentMode();
        panel.showRule(mode, mode == null ? service.getStrategy().name() : describe(mode));
        showLivePreview();
    }

    // ---- Beneficiaries ----

    private void registerBeneficiary() {
        Form form = beneficiaryForm("Register a beneficiary", "", "", "", ContactPreference.SMS, "", "");
        dialogs.form(form).ifPresent(values -> {
            // The Beneficiary constructor refuses a blank name or area and a household below 1.
            Beneficiary beneficiary = service.registerBeneficiary(new Beneficiary(values.text("name"),
                    Inputs.orNull(values.text("email")), Inputs.orNull(values.text("phone")),
                    values.choice("contact", ContactPreference.class), values.text("area"),
                    Inputs.whole(values.text("household"), "Household size")));
            refresh(); // so the new row is in the table and can be selected
            panel.selectBeneficiary(beneficiary.getId());
            panel.showStatus("Registered " + beneficiary.getName() + " as " + beneficiary.getId() + ".");
        });
    }

    private void editBeneficiary() {
        Beneficiary b = requireSelectedBeneficiary();
        Form form = beneficiaryForm("Edit " + b.getName() + " (" + b.getId() + ")", b.getName(), b.getEmail(),
                b.getPhone(), b.getPreferredContact(), b.getArea(), String.valueOf(b.getHouseholdSize()));
        dialogs.form(form).ifPresent(values -> {
            // The service checks everything first, so a rejected update changes nothing.
            service.updateBeneficiary(b.getId(), values.text("name"), Inputs.orNull(values.text("email")),
                    Inputs.orNull(values.text("phone")), values.choice("contact", ContactPreference.class),
                    values.text("area"), Inputs.whole(values.text("household"), "Household size"));
            panel.showStatus("Updated the details of " + b.getName() + ".");
        });
    }

    private static Form beneficiaryForm(String title, String name, String email, String phone,
                                        ContactPreference contact, String area, String household) {
        return new Form(title)
                .text("name", "Name", name)
                .text("email", "E-mail", email, "optional")
                .text("phone", "Phone", phone)
                .choice("contact", "Contact by", List.of(ContactPreference.values()), contact)
                .text("area", "Area", area, "Imbaba")
                .text("household", "Household size", household, "4");
    }

    private void removeBeneficiary() {
        Beneficiary b = requireSelectedBeneficiary();
        if (dialogs.confirm("Remove " + b.getName() + " (" + b.getId() + ") and their needs? "
                + "Someone who has already received anything is kept for the reports.")
                && service.removeBeneficiary(b.getId())) {
            panel.showStatus("Removed " + b.getName() + " and their needs.");
        }
    }

    // ---- Reports (Template Method in the model) ----

    private void progressReport() {
        Beneficiary b = requireSelectedBeneficiary();
        dialogs.text("Progress report - " + b.getName(), service.progressReport(b.getId(), LocalDate.now()));
    }

    private void impactReport() {
        List<String> programs = service.listPrograms();
        if (programs.isEmpty()) {
            throw new IllegalStateException("Nothing has been handed out yet, so there is no program to report on");
        }
        Form form = new Form("Program impact report")
                .choice("program", "Program", programs, programs.get(0))
                .date("date", "As of", LocalDate.now());
        dialogs.form(form).ifPresent(values -> {
            String program = values.choice("program", String.class);
            LocalDate date = values.date("date") == null ? LocalDate.now() : values.date("date");
            dialogs.text("Program impact report - " + program, service.programImpactReport(program, date));
        });
    }

    // ---- Needs ----

    private void addNeed() {
        List<Beneficiary> people = service.listBeneficiaries();
        if (people.isEmpty()) {
            throw new IllegalStateException("Register a beneficiary first");
        }
        Beneficiary selected = panel.selectedBeneficiary();
        Form form = new Form("Add a need")
                .choice("beneficiary", "Beneficiary", people, selected == null ? people.get(0) : selected)
                .text("category", "Category", "", "food parcel, blanket, counselling session")
                .text("quantity", "Quantity", "", "4")
                .choice("urgency", "Urgency", List.of(Urgency.values()), Urgency.MEDIUM)
                .date("requested", "Requested on", LocalDate.now());
        dialogs.form(form).ifPresent(values -> {
            Beneficiary b = values.choice("beneficiary", Beneficiary.class);
            if (b == null) {
                throw new IllegalArgumentException("Beneficiary is required");
            }
            // A HIGH need is announced to the volunteers by the service (URGENT_NEED).
            Need need = service.addNeed(b.getId(), values.text("category"),
                    Inputs.whole(values.text("quantity"), "Quantity"), values.choice("urgency", Urgency.class),
                    values.date("requested"));
            refresh();
            panel.selectNeed(need.getId());
            panel.showStatus("Added " + need.getId() + ": " + need.getQuantity() + " x " + need.getCategory()
                    + " for " + b.getName() + (need.getUrgency() == Urgency.HIGH ? " (urgent alert sent)." : "."));
        });
    }

    private void editNeed() {
        Need need = requireSelectedNeed();
        Form form = new Form("Edit " + need.getId())
                .header(need.getCategory() + " for " + need.getBeneficiary().getName() + ", "
                        + need.getReceived() + " received so far")
                .text("quantity", "Quantity", String.valueOf(need.getQuantity()))
                .choice("urgency", "Urgency", List.of(Urgency.values()), need.getUrgency());
        dialogs.form(form).ifPresent(values -> {
            Urgency before = need.getUrgency();
            service.updateNeed(need.getId(), Inputs.whole(values.text("quantity"), "Quantity"),
                    values.choice("urgency", Urgency.class));
            boolean alerted = before != Urgency.HIGH && need.getUrgency() == Urgency.HIGH && !need.isMet();
            panel.showStatus("Updated " + need.getId() + (alerted ? " (urgent alert sent)." : "."));
        });
    }

    private void removeNeed() {
        Need need = requireSelectedNeed();
        if (dialogs.confirm("Remove " + need.getId() + " (" + need.getCategory() + " for "
                + need.getBeneficiary().getName() + ")?")) {
            service.removeNeed(need.getId());
            panel.showStatus("Removed " + need.getId() + ".");
        }
    }

    private void giveDirectly() {
        Need need = requireSelectedNeed();
        Form form = new Form("Give directly")
                .header(need.getId() + " " + need.getCategory() + " for " + need.getBeneficiary().getName() + ": "
                        + need.remaining() + " still needed")
                .text("units", "Units to give", "", String.valueOf(Math.max(need.remaining(), 1)))
                .text("program", "Program", lastProgram, programHint())
                .date("date", "Date", LocalDate.now());
        dialogs.form(form).ifPresent(values -> {
            int units = Inputs.whole(values.text("units"), "Units");
            Distribution d = service.giveDirectly(need.getId(), units, values.text("program"), values.date("date"));
            lastProgram = d.getProgram();
            panel.showStatus("Recorded " + d.getId() + ": " + d.getQuantity() + " x " + d.getCategory() + " to "
                    + d.getBeneficiary().getName() + " (" + d.getProgram() + ").");
        });
    }

    // ---- Allocation (Strategy in the model) ----

    private void changeRule(AllocationMode mode) {
        service.setStrategy(mode);
        panel.showStatus("Allocation rule is now: " + service.getStrategy().name() + ".");
    }

    /** Shows how the current rule would split some stock, without handing anything out. */
    private void preview() {
        List<String> categories = openCategories();
        Form form = new Form("Preview a split")
                .header("Rule in use: " + service.getStrategy().name() + ". Nothing is handed out.")
                .choice("category", "Category", categories, initialCategory(categories))
                .text("units", "Units available", lastUnits > 0 ? String.valueOf(lastUnits) : "", "10");
        dialogs.form(form).ifPresent(values -> {
            String category = requireCategory(values.choice("category", String.class));
            int units = Inputs.whole(values.text("units"), "Units available");
            Map<Need, Integer> shares = service.preview(category, units);
            lastCategory = category;
            lastUnits = units;
            dialogs.text("Preview - " + service.getStrategy().name(),
                    "How \"" + service.getStrategy().name() + "\" would share " + units + " x " + category + ":\n\n"
                            + splitTable(shares) + "\n\n" + handedOut(shares, units)
                            + "\nPreview only: nothing was recorded. Pick another rule and preview again to compare.");
            panel.showStatus("Previewed " + units + " x " + category + " with " + service.getStrategy().name() + ".");
        });
    }

    /** Asks for the stock, shows the split the current rule would make, and hands it out only after OK. */
    private void distribute() {
        List<String> categories = openCategories();
        Form form = new Form("Distribute stock")
                .header("Rule in use: " + service.getStrategy().name() + ". You see the split before anything "
                        + "is handed out.")
                .text("program", "Program", lastProgram, programHint())
                .choice("category", "Category", categories, initialCategory(categories))
                .text("units", "Units available", lastUnits > 0 ? String.valueOf(lastUnits) : "", "10")
                .date("date", "Date", LocalDate.now());
        dialogs.form(form).ifPresent(values -> {
            String program = values.text("program");
            String category = requireCategory(values.choice("category", String.class));
            int units = Inputs.whole(values.text("units"), "Units available");
            LocalDate date = values.date("date");
            // Missing answers are refused before the confirmation, not after it.
            if (program.isEmpty()) {
                throw new IllegalArgumentException("Program name is required");
            }
            if (date == null) {
                throw new IllegalArgumentException("Distribution date is required");
            }
            Map<Need, Integer> shares = service.preview(category, units);
            lastCategory = category;
            lastUnits = units;
            lastProgram = program;
            if (!dialogs.confirm(service.getStrategy().name() + " would share " + units + " x " + category + ":\n\n"
                    + splitLines(shares) + "\n" + handedOut(shares, units)
                    + "\n\nHand this out under \"" + program + "\" on " + date + "?")) {
                panel.showStatus("Nothing was handed out.");
                return;
            }
            List<Distribution> made = service.distribute(program, category, units, date);
            int total = 0;
            for (Distribution d : made) {
                total += d.getQuantity();
            }
            panel.showStatus(made.size() + " distribution(s) recorded: " + total + " of " + units + " x " + category
                    + " handed out with " + service.getStrategy().name() + ".");
        });
    }

    /** The rule bar shows, for the last stock the user tried, who the current rule would give it to. */
    private void showLivePreview() {
        if (lastCategory == null) {
            panel.showPreview("Preview a split to see who the current rule gives limited stock to.");
            return;
        }
        Map<Need, Integer> shares = service.preview(lastCategory, lastUnits);
        if (shares.isEmpty()) {
            panel.showPreview("Nobody has an open need for " + lastCategory + " any more.");
            return;
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<Need, Integer> share : shares.entrySet()) {
            parts.add(share.getKey().getBeneficiary().getName() + " " + share.getValue());
        }
        panel.showPreview(lastUnits + " x " + lastCategory + " now: " + String.join(", ", parts));
    }

    private static String splitTable(Map<Need, Integer> shares) {
        StringBuilder sb = new StringBuilder(String.format("  %-7s %-18s %-7s %12s %10s%n",
                "Need", "Beneficiary", "Urgency", "Still needed", "Would get"));
        for (Map.Entry<Need, Integer> share : shares.entrySet()) {
            Need need = share.getKey();
            sb.append(String.format("  %-7s %-18s %-7s %12d %10d%n", need.getId(), need.getBeneficiary().getName(),
                    need.getUrgency(), need.remaining(), share.getValue()));
        }
        return sb.toString().stripTrailing();
    }

    private static String splitLines(Map<Need, Integer> shares) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Need, Integer> share : shares.entrySet()) {
            Need need = share.getKey();
            sb.append(String.format("  %s %s gets %d (needs %d, %s)%n", need.getId(), need.getBeneficiary().getName(),
                    share.getValue(), need.remaining(), need.getUrgency()));
        }
        return sb.toString();
    }

    private static String handedOut(Map<Need, Integer> shares, int units) {
        int total = 0;
        for (int value : shares.values()) {
            total += value;
        }
        return total + " of " + units + " unit(s) handed out" + (total < units ? ", " + (units - total) + " left over." : ".");
    }

    // ---- Helpers ----

    /** The categories someone still needs, each once, in the order first recorded. */
    private List<String> openCategories() {
        List<String> categories = new ArrayList<>();
        for (Need need : service.openNeeds()) {
            if (!categories.contains(need.getCategory())) {
                categories.add(need.getCategory());
            }
        }
        if (categories.isEmpty()) {
            throw new IllegalStateException("Nobody has an open need right now");
        }
        return categories;
    }

    private String initialCategory(List<String> categories) {
        return categories.contains(lastCategory) ? lastCategory : categories.get(0);
    }

    private static String requireCategory(String category) {
        if (category == null) {
            throw new IllegalArgumentException("Category is required");
        }
        return category;
    }

    private String programHint() {
        List<String> programs = service.listPrograms();
        return programs.isEmpty() ? "Winter Food Program" : String.join(", ", programs);
    }

    /** Every need, met or not, in the order they were recorded (NED-1, NED-2, ...). */
    private List<Need> allNeeds() {
        List<Need> all = new ArrayList<>();
        for (Beneficiary b : service.listBeneficiaries()) {
            all.addAll(b.getNeeds());
        }
        all.sort(Comparator.comparingInt(BeneficiaryGuiController::idNumber));
        return all;
    }

    private static int idNumber(Need need) {
        String id = need.getId();
        return Integer.parseInt(id.substring(id.indexOf('-') + 1));
    }

    private static String describe(AllocationMode mode) {
        switch (mode) {
            case URGENCY:
                return "High-urgency needs are filled first; needs of equal urgency go to whoever asked first.";
            case FIRST_COME:
                return "Needs are filled fully in the order they were requested, whatever their urgency.";
            default:
                return "One unit at a time to each need in turn, so everyone gets some before anyone gets more.";
        }
    }

    /** The menu entry for the strategy in use, or null if it is none of them. */
    private AllocationMode currentMode() {
        for (AllocationMode mode : AllocationMode.values()) {
            if (mode.create().getClass() == service.getStrategy().getClass()) {
                return mode;
            }
        }
        return null;
    }

    private Beneficiary requireSelectedBeneficiary() {
        Beneficiary beneficiary = panel.selectedBeneficiary();
        if (beneficiary == null) {
            throw new IllegalArgumentException("Select a beneficiary in the table first.");
        }
        return beneficiary;
    }

    private Need requireSelectedNeed() {
        Need need = panel.selectedNeed();
        if (need == null) {
            throw new IllegalArgumentException("Select a need in the table first.");
        }
        return need;
    }
}
