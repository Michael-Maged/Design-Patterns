package org.nonprofit.gui.donation;

import java.util.List;
import java.util.Locale;

import org.nonprofit.donation.model.Campaign;
import org.nonprofit.donation.factorymethod.Donation;
import org.nonprofit.donation.model.Donor;
import org.nonprofit.gui.common.Ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * MVC pattern (view) of the Donations screen in the GUI: the donors,
 * donations and campaigns tables, their buttons and the bar that shows
 * which creator builds each type of donation. It only shows what it is
 * given and reports clicks; {@link DonationGuiController} decides what
 * happens.
 */
public class DonationPanel extends BorderPane {
    private final TableView<Donor> donors = Ui.table("donations-donor-table", "No donors yet.");
    private final TableView<Donation> donations = Ui.table("donations-table", "No donations yet.");
    private final TableView<Campaign> campaigns = Ui.table("donations-campaign-table", "No campaigns yet.");

    private final Button addDonor = Ui.button("Add donor...", "donations-donor-add");
    private final Button editDonor = Ui.button("Edit contact...", "donations-donor-edit");
    private final Button removeDonor = Ui.button("Remove", "donations-donor-remove");
    private final Button history = Ui.button("Donation history", "donations-donor-history");
    private final Button taxStatement = Ui.button("Tax statement...", "donations-tax-statement");
    private final Button summary = Ui.button("Summary and top donors...", "donations-summary");

    private final Button donate = Ui.button("Make a donation...", "donations-donate");
    private final Button receipt = Ui.button("Receipt", "donations-receipt");

    private final Button addCampaign = Ui.button("Add campaign...", "donations-campaign-add");

    private final Label creators = new Label();
    private final Label lastBuilt = new Label();
    private final Label status = Ui.statusBar("donations-status");

    public DonationPanel() {
        setId("donations-screen");
        getStyleClass().add("screen");
        buildTables();

        creators.setId("donations-creators");
        creators.getStyleClass().add("muted");
        lastBuilt.setId("donations-last-built");
        lastBuilt.getStyleClass().add("highlight");

        Label creatorsTitle = new Label("Donation types (Factory Method):");
        creatorsTitle.getStyleClass().add("info-bar-title");
        HBox creatorsRow = new HBox(10, creatorsTitle, creators);
        creatorsRow.setAlignment(Pos.CENTER_LEFT);
        Label lastTitle = new Label("Last donation:");
        HBox lastRow = new HBox(10, lastTitle, lastBuilt);
        lastRow.setAlignment(Pos.CENTER_LEFT);
        VBox factoryBar = new VBox(6, creatorsRow, lastRow);
        factoryBar.getStyleClass().add("info-bar");

        VBox top = new VBox(10, Ui.header("Donations",
                "Donors, donations and campaigns. Each donation type is built by its own creator; "
                        + "receipts and tax statements share one document layout."),
                factoryBar);

        VBox donorSection = Ui.section("Donors",
                Ui.toolbar(addDonor, editDonor, removeDonor, Ui.gap(), history, taxStatement),
                donors);
        VBox campaignSection = Ui.section("Campaigns", Ui.toolbar(addCampaign), campaigns);
        VBox donationSection = Ui.section("Donations",
                Ui.toolbar(donate, receipt, Ui.gap(), summary), donations);

        SplitPane upper = new SplitPane(donorSection, campaignSection);
        upper.setOrientation(Orientation.HORIZONTAL);
        upper.setDividerPositions(0.64);
        SplitPane tables = new SplitPane(upper, donationSection);
        tables.setOrientation(Orientation.VERTICAL);
        tables.setDividerPositions(0.46);

        setTop(top);
        setCenter(tables);
        setBottom(status);
        BorderPane.setMargin(tables, new Insets(10, 0, 8, 0));
    }

    private void buildTables() {
        donors.getColumns().add(Ui.column("ID", 55, Donor::getId));
        donors.getColumns().add(Ui.column("Name", 110, Donor::getName));
        donors.getColumns().add(Ui.column("E-mail", 150, Donor::getEmail));
        donors.getColumns().add(Ui.column("Phone", 100, Donor::getPhone));
        donors.getColumns().add(Ui.column("Contact by", 75, Donor::getPreferredContact));
        donors.getColumns().add(Ui.column("Donations", 65, d -> d.getDonations().size()));
        donors.getColumns().add(Ui.column("Total given", 80, d -> Donation.money(d.getTotalGiven())));

        donations.getColumns().add(Ui.column("ID", 60, Donation::getId));
        donations.getColumns().add(Ui.column("Date", 85, Donation::getDate));
        donations.getColumns().add(Ui.column("Donor", 120, d -> d.getDonor().getName()));
        donations.getColumns().add(Ui.column("Type", 70, Donation::getType));
        donations.getColumns().add(Ui.column("Amount", 85, d -> Donation.money(d.getAmount())));
        donations.getColumns().add(Ui.column("Details", 260, Donation::getDetails));
        donations.getColumns().add(Ui.column("Campaign", 130,
                d -> d.getCampaign() == null ? "" : d.getCampaign().getName()));

        campaigns.getColumns().add(Ui.column("Campaign", 100, Campaign::getName));
        campaigns.getColumns().add(Ui.column("Goal", 80, c -> Donation.money(c.getGoal())));
        campaigns.getColumns().add(Ui.column("Raised", 80, c -> Donation.money(c.getRaised())));
        campaigns.getColumns().add(progressColumn());
    }

    /** A bar filled up to the goal, with the percentage (which can pass 100) next to it. */
    private static TableColumn<Campaign, Campaign> progressColumn() {
        TableColumn<Campaign, Campaign> column = new TableColumn<>("Progress");
        column.setPrefWidth(105);
        column.setMinWidth(95);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(c -> new TableCell<Campaign, Campaign>() {
            private final ProgressBar bar = new ProgressBar();

            {
                bar.setMaxWidth(Double.MAX_VALUE);
                bar.setPrefWidth(50);
                bar.setMinWidth(30);
                setContentDisplay(ContentDisplay.LEFT);
                setGraphicTextGap(6);
            }

            @Override
            protected void updateItem(Campaign campaign, boolean empty) {
                super.updateItem(campaign, empty);
                if (empty || campaign == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                bar.setProgress(Math.min(1.0, campaign.getProgress() / 100));
                setGraphic(bar);
                setText(String.format(Locale.ROOT, "%.0f%%", campaign.getProgress()));
            }
        });
        return column;
    }

    // ---- What the controller shows ----

    public void showDonors(List<Donor> rows) {
        Ui.show(donors, rows, Donor::getId);
        donors.refresh();
    }

    public void showDonations(List<Donation> rows) {
        Ui.show(donations, rows, Donation::getId);
        donations.refresh();
    }

    public void showCampaigns(List<Campaign> rows) {
        Ui.show(campaigns, rows, Campaign::getName);
        campaigns.refresh();
    }

    /** The donation types and the creator that builds each one. */
    public void showCreators(String text) {
        creators.setText(text);
    }

    /** Which creator built the newest donation. */
    public void showLastBuilt(String text) {
        lastBuilt.setText(text);
    }

    public void showStatus(String text) {
        status.setText(text);
    }

    // ---- What the user selected ----

    /** @return the selected donor, or null */
    public Donor selectedDonor() {
        return donors.getSelectionModel().getSelectedItem();
    }

    /** @return the selected donation, or null */
    public Donation selectedDonation() {
        return donations.getSelectionModel().getSelectedItem();
    }

    /** @return the selected campaign, or null */
    public Campaign selectedCampaign() {
        return campaigns.getSelectionModel().getSelectedItem();
    }

    public boolean selectDonor(String id) {
        return Ui.select(donors, Donor::getId, id);
    }

    public boolean selectDonation(String id) {
        return Ui.select(donations, Donation::getId, id);
    }

    public boolean selectCampaign(String name) {
        return Ui.select(campaigns, Campaign::getName, name);
    }

    // ---- What the user can do (the controller says what each one means) ----

    public void onAddDonor(Runnable action) { addDonor.setOnAction(e -> action.run()); }
    public void onEditDonor(Runnable action) { editDonor.setOnAction(e -> action.run()); }
    public void onRemoveDonor(Runnable action) { removeDonor.setOnAction(e -> action.run()); }
    public void onHistory(Runnable action) { history.setOnAction(e -> action.run()); }
    public void onTaxStatement(Runnable action) { taxStatement.setOnAction(e -> action.run()); }
    public void onSummary(Runnable action) { summary.setOnAction(e -> action.run()); }

    public void onDonate(Runnable action) { donate.setOnAction(e -> action.run()); }
    public void onReceipt(Runnable action) { receipt.setOnAction(e -> action.run()); }

    public void onAddCampaign(Runnable action) { addCampaign.setOnAction(e -> action.run()); }
}
