package org.nonprofit.gui.donation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.nonprofit.core.model.ContactPreference;
import org.nonprofit.donation.model.Campaign;
import org.nonprofit.donation.factorymethod.Donation;
import org.nonprofit.donation.model.DonationForm;
import org.nonprofit.donation.model.DonationService;
import org.nonprofit.donation.model.DonationType;
import org.nonprofit.donation.model.Donor;
import org.nonprofit.gui.common.Dialogs;
import org.nonprofit.gui.common.Feed;
import org.nonprofit.gui.common.Form;
import org.nonprofit.gui.common.FormValues;
import org.nonprofit.gui.common.Inputs;
import org.nonprofit.gui.common.ScreenActions;
import org.nonprofit.gui.common.Ui;

/**
 * MVC pattern (controller) of the Donations screen in the GUI: turns each
 * click on the {@link DonationPanel} into calls on the model
 * ({@link DonationService}, unchanged from the console version) and tells
 * the panel what to show. Rules broken in the model come back as
 * exceptions and are shown as error dialogs by {@link ScreenActions}.
 *
 * A new donation is built by the service's FACTORY METHOD (one creator per
 * type, picked in the first form); receipts and tax statements come from
 * the module's TEMPLATE METHOD.
 */
public class DonationGuiController {
    private static final String NO_CAMPAIGN = "None";
    private static final int TOP_DONORS = 3;

    private final DonationService service;
    private final DonationPanel panel;
    private final Dialogs dialogs;
    private final ScreenActions actions;

    /**
     * @param deliveries the messages Communication sent since the last call
     *                   ({@code CommunicationService::takeOutput}); this module does not import Communication
     */
    public DonationGuiController(DonationService service, Dialogs dialogs, Feed feed,
                                 Supplier<List<String>> deliveries) {
        this.service = service;
        this.dialogs = dialogs;
        this.panel = new DonationPanel();
        this.actions = new ScreenActions(dialogs, feed, deliveries, this::refresh);

        panel.onAddDonor(() -> actions.run(this::addDonor));
        panel.onEditDonor(() -> actions.run(this::editDonor));
        panel.onRemoveDonor(() -> actions.run(this::removeDonor));
        panel.onHistory(() -> actions.run(this::history));
        panel.onTaxStatement(() -> actions.run(this::taxStatement));
        panel.onSummary(() -> actions.run(this::summary));

        panel.onDonate(() -> actions.run(this::donate));
        panel.onReceipt(() -> actions.run(this::receipt));

        panel.onAddCampaign(() -> actions.run(this::addCampaign));
        refresh();
    }

    public DonationPanel getPanel() {
        return panel;
    }

    /** Reloads the three tables and the Factory Method bar from the service. */
    public void refresh() {
        panel.showDonors(service.getDonors());
        panel.showDonations(service.getDonations());
        panel.showCampaigns(service.getCampaigns());
        panel.showCreators(creatorList());
        List<Donation> all = service.getDonations();
        panel.showLastBuilt(all.isEmpty() ? "none yet." : builtBy(all.get(all.size() - 1)));
    }

    // ---- Donors ----

    private void addDonor() {
        Form form = new Form("Add a donor")
                .text("name", "Name", "")
                .text("email", "E-mail", "")
                .text("phone", "Phone", "")
                .choice("contact", "Contact by", List.of(ContactPreference.values()), ContactPreference.EMAIL);
        dialogs.form(form).ifPresent(values -> {
            Donor donor = service.addDonor(values.text("name"), Inputs.orNull(values.text("email")),
                    Inputs.orNull(values.text("phone")), values.choice("contact", ContactPreference.class));
            refresh(); // so the new row is in the table and can be selected
            panel.selectDonor(donor.getId());
            panel.showStatus("Added " + donor.getName() + " as " + donor.getId() + ".");
        });
    }

    private void editDonor() {
        Donor donor = requireSelectedDonor();
        Form form = new Form("Edit " + donor.getName() + " (" + donor.getId() + ")")
                .text("name", "Name", donor.getName())
                .text("email", "E-mail", donor.getEmail())
                .text("phone", "Phone", donor.getPhone())
                .choice("contact", "Contact by", List.of(ContactPreference.values()), donor.getPreferredContact());
        dialogs.form(form).ifPresent(values -> {
            Donor updated = service.updateDonor(donor.getId(), values.text("name"),
                    Inputs.orNull(values.text("email")), Inputs.orNull(values.text("phone")),
                    values.choice("contact", ContactPreference.class));
            panel.showStatus("Updated the contact details of " + updated.getName() + ".");
        });
    }

    private void removeDonor() {
        Donor donor = requireSelectedDonor();
        if (dialogs.confirm("Remove " + donor.getName() + " (" + donor.getId() + ")?")) {
            service.removeDonor(donor.getId());
            panel.showStatus("Removed " + donor.getName() + ".");
        }
    }

    private void history() {
        Donor donor = requireSelectedDonor();
        StringBuilder text = new StringBuilder(donor.summary()).append("\n\n");
        List<Donation> list = donor.getDonations();
        if (list.isEmpty()) {
            text.append("No donations yet.");
        }
        for (Donation donation : list) {
            text.append(donation).append("\n");
        }
        dialogs.text("Donation history - " + donor.getName(), text.toString().trim());
    }

    // ---- Documents (Template Method in the model) ----

    private void taxStatement() {
        Donor donor = requireSelectedDonor();
        Form form = new Form("Tax statement")
                .header("For " + donor.getName() + " (" + donor.getId() + ")")
                .text("year", "Year", String.valueOf(LocalDate.now().getYear()), "2026");
        dialogs.form(form).ifPresent(values -> {
            int year = Inputs.whole(values.text("year"), "Year");
            dialogs.text("Tax statement " + year + " - " + donor.getName(),
                    service.makeTaxStatement(donor.getId(), year, LocalDate.now()));
        });
    }

    private void receipt() {
        Donation donation = requireSelectedDonation();
        dialogs.text("Receipt - " + donation.getId(), service.makeReceipt(donation.getId(), LocalDate.now()));
    }

    // ---- Reports ----

    private void summary() {
        Form form = new Form("Summary and top donors")
                .text("count", "How many top donors", String.valueOf(TOP_DONORS), "3");
        dialogs.form(form).ifPresent(values -> {
            int count = Inputs.whole(values.text("count"), "Number of top donors");
            if (count < 1) {
                throw new IllegalArgumentException("Number of top donors must be at least 1");
            }
            StringBuilder text = new StringBuilder(service.summaryReport());
            text.append("\n\nTop ").append(count).append(" donor(s)\n");
            int rank = 1;
            for (Donor donor : service.getTopDonors(count)) {
                text.append(String.format("  %d. %s%n", rank++, donor.summary()));
            }
            dialogs.text("Summary and top donors", text.toString().trim());
        });
    }

    // ---- Donations (Factory Method in the model) ----

    /**
     * Two forms: the first picks the donor and the type, the second asks only
     * the fields that type needs, then the service hands the form to the
     * creator for that type.
     */
    private void donate() {
        List<Donor> donors = service.getDonors();
        if (donors.isEmpty()) {
            throw new IllegalStateException("Add a donor first");
        }
        Form choose = new Form("Make a donation")
                .header("Each type is built by its own creator (Factory Method).")
                .choice("donor", "Donor", donors, panel.selectedDonor())
                .choice("type", "Type", List.of(DonationType.values()), DonationType.ONLINE);
        dialogs.form(choose).ifPresent(first -> {
            Donor donor = first.choice("donor", Donor.class);
            DonationType type = first.choice("type", DonationType.class);
            if (donor == null) {
                throw new IllegalArgumentException("Donor is required");
            }
            if (type == null) {
                throw new IllegalArgumentException("Donation type is required");
            }
            dialogs.form(detailsForm(donor, type)).ifPresent(values -> record(donor, type, values));
        });
    }

    /** The second form: the fields for this type, then amount, campaign and date. */
    private Form detailsForm(Donor donor, DonationType type) {
        Form form = new Form("Make " + article(type) + " " + Ui.display(type).toLowerCase(Locale.ROOT) + " donation")
                .header("Factory Method: " + creatorName(type) + " builds it. Donor: " + donor.getName()
                        + " (" + donor.getId() + ")");
        switch (type) {
            case ONLINE:
                form.text("reference", "Payment reference", "", "PAY-1234")
                        .check("monthly", "Repeat every month", false);
                break;
            case CHECK:
                form.text("reference", "Check number", "", "100245")
                        .text("bank", "Bank", "", "optional");
                break;
            case IN_KIND:
                form.text("item", "Items given", "", "40 blankets");
                break;
            default:
                break;
        }
        List<String> campaigns = new ArrayList<>();
        campaigns.add(NO_CAMPAIGN);
        for (Campaign campaign : service.getCampaigns()) {
            campaigns.add(campaign.getName());
        }
        return form.text("amount", type == DonationType.IN_KIND ? "Estimated value" : "Amount", "", "250")
                .choice("campaign", "Campaign", campaigns, NO_CAMPAIGN)
                .date("date", "Date received", LocalDate.now());
    }

    private void record(Donor donor, DonationType type, FormValues values) {
        DonationForm form = new DonationForm(Inputs.number(values.text("amount").replace(",", "").replace("$", ""),
                type == DonationType.IN_KIND ? "Estimated value" : "Amount"));
        switch (type) {
            case ONLINE:
                form.setReference(values.text("reference"));
                form.setMonthly(values.check("monthly"));
                break;
            case CHECK:
                form.setReference(values.text("reference"));
                form.setBank(Inputs.orNull(values.text("bank")));
                break;
            case IN_KIND:
                form.setItem(values.text("item"));
                break;
            default:
                break;
        }
        String campaign = values.choice("campaign", String.class);
        form.setCampaign(campaign == null || campaign.equals(NO_CAMPAIGN) ? null : campaign);
        LocalDate date = values.date("date") == null ? LocalDate.now() : values.date("date");
        // The service refuses a date after today (a backdated check is fine).
        Donation donation = service.donate(donor.getId(), type, date, form, LocalDate.now());
        refresh();
        panel.selectDonation(donation.getId());
        panel.selectDonor(donor.getId());
        panel.showStatus(builtBy(donation));
    }

    // ---- Campaigns ----

    private void addCampaign() {
        Form form = new Form("Add a campaign")
                .text("name", "Campaign name", "", "Winter Blankets")
                .text("goal", "Goal amount", "", "15000");
        dialogs.form(form).ifPresent(values -> {
            Campaign campaign = service.addCampaign(values.text("name"),
                    Inputs.number(values.text("goal").replace(",", "").replace("$", ""), "Goal"));
            refresh();
            panel.selectCampaign(campaign.getName());
            panel.showStatus("Added the campaign " + campaign + ".");
        });
    }

    // ---- Helpers ----

    /** e.g. "OnlineDonationCreator built OnlineDonation DON-5 for Hany Mansour ($250.00)." */
    private String builtBy(Donation donation) {
        return creatorName(donation.getType()) + " built " + donation.getClass().getSimpleName() + " "
                + donation.getId() + " for " + donation.getDonor().getName() + " ("
                + Donation.money(donation.getAmount()) + ").";
    }

    /** "Online → OnlineDonationCreator   Check → ..." */
    private String creatorList() {
        List<String> parts = new ArrayList<>();
        for (DonationType type : DonationType.values()) {
            parts.add(Ui.display(type) + " → " + creatorName(type));
        }
        return String.join("     ", parts);
    }

    private String creatorName(DonationType type) {
        return service.getCreator(type).getClass().getSimpleName();
    }

    private static String article(DonationType type) {
        return type == DonationType.ONLINE || type == DonationType.IN_KIND ? "an" : "a";
    }

    private Donor requireSelectedDonor() {
        Donor donor = panel.selectedDonor();
        if (donor == null) {
            throw new IllegalArgumentException("Select a donor in the table first.");
        }
        return donor;
    }

    private Donation requireSelectedDonation() {
        Donation donation = panel.selectedDonation();
        if (donation == null) {
            throw new IllegalArgumentException("Select a donation in the table first.");
        }
        return donation;
    }
}
