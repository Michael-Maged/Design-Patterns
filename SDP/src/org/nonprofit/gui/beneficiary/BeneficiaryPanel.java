package org.nonprofit.gui.beneficiary;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import org.nonprofit.beneficiary.strategy.AllocationMode;
import org.nonprofit.beneficiary.model.Beneficiary;
import org.nonprofit.beneficiary.model.Distribution;
import org.nonprofit.beneficiary.model.Need;
import org.nonprofit.beneficiary.model.Urgency;
import org.nonprofit.gui.common.Ui;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * MVC pattern (view) of the Beneficiaries screen in the GUI: the
 * beneficiaries, needs and distributions tables, their buttons and the
 * allocation-rule bar. It only shows what it is given and reports clicks;
 * {@link BeneficiaryGuiController} decides what happens.
 */
public class BeneficiaryPanel extends BorderPane {
    /** Inline style of the badge that marks a HIGH-urgency need (readable on a selected row too). */
    private static final String URGENT_BADGE = "-fx-background-color: #fde2e2; -fx-text-fill: #b91c1c;"
            + " -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 0 6 0 6;";

    private final TableView<Beneficiary> beneficiaries =
            Ui.table("beneficiaries-table", "No beneficiaries yet.");
    private final TableView<Need> needs = Ui.table("beneficiaries-need-table", "No needs yet.");
    private final TableView<Distribution> distributions =
            Ui.table("beneficiaries-distribution-table", "Nothing handed out yet.");

    private final Button register = Ui.button("Register...", "beneficiaries-register");
    private final Button edit = Ui.button("Edit...", "beneficiaries-edit");
    private final Button remove = Ui.button("Remove", "beneficiaries-remove");
    private final Button progressReport = Ui.button("Progress report", "beneficiaries-progress-report");

    private final Button addNeed = Ui.button("Add need...", "beneficiaries-need-add");
    private final Button editNeed = Ui.button("Edit...", "beneficiaries-need-edit");
    private final Button removeNeed = Ui.button("Remove", "beneficiaries-need-remove");
    private final Button giveDirectly = Ui.button("Give directly...", "beneficiaries-need-give");
    private final CheckBox openOnly = new CheckBox("Open needs only");

    private final Button impactReport = Ui.button("Program impact report...", "beneficiaries-impact-report");

    private final ComboBox<AllocationMode> rule = new ComboBox<>();
    private final Label ruleName = new Label();
    private final Button preview = Ui.button("Preview split...", "beneficiaries-preview");
    private final Button distribute = Ui.button("Distribute stock...", "beneficiaries-distribute");
    private final Label previewResult = new Label();
    private final Label status = Ui.statusBar("beneficiaries-status");

    /** True while the controller sets the rule, so that is not reported as the user's choice. */
    private boolean showingRule;
    private Consumer<AllocationMode> ruleListener = mode -> { };

    /**
     * @param rules     the rules offered in the drop-down
     * @param ruleLabel the text shown for each rule, e.g. "Urgency first"
     */
    public BeneficiaryPanel(List<AllocationMode> rules, Function<AllocationMode, String> ruleLabel) {
        setId("beneficiaries-screen");
        getStyleClass().add("screen");
        buildTables();

        rule.setId("beneficiaries-rule");
        rule.getItems().setAll(rules);
        rule.setConverter(new StringConverter<AllocationMode>() {
            @Override
            public String toString(AllocationMode mode) {
                return mode == null ? "" : ruleLabel.apply(mode);
            }

            @Override
            public AllocationMode fromString(String text) {
                return null;
            }
        });
        rule.valueProperty().addListener((property, old, chosen) -> {
            if (!showingRule && chosen != null) {
                ruleListener.accept(chosen);
            }
        });
        ruleName.setId("beneficiaries-rule-name");
        ruleName.getStyleClass().add("muted");
        previewResult.setId("beneficiaries-preview-result");
        previewResult.getStyleClass().add("highlight");
        openOnly.setId("beneficiaries-open-only");

        Label ruleTitle = new Label("Allocation rule (Strategy):");
        ruleTitle.getStyleClass().add("info-bar-title");
        HBox ruleRow = new HBox(10, ruleTitle, rule, ruleName);
        ruleRow.setAlignment(Pos.CENTER_LEFT);
        Label stockTitle = new Label("Limited stock:");
        HBox stockRow = new HBox(10, stockTitle, preview, distribute, previewResult);
        stockRow.setAlignment(Pos.CENTER_LEFT);
        VBox ruleBar = new VBox(6, ruleRow, stockRow);
        ruleBar.getStyleClass().add("info-bar");

        VBox top = new VBox(10, Ui.header("Beneficiaries",
                "People and households, their needs and what they received. Pick a rule to change how limited "
                        + "stock is shared out."),
                ruleBar);

        VBox beneficiarySection = Ui.section("Beneficiaries",
                Ui.toolbar(register, edit, remove, Ui.gap(), progressReport),
                beneficiaries);
        VBox needSection = Ui.section("Needs",
                Ui.toolbar(addNeed, editNeed, removeNeed, Ui.gap(), giveDirectly, Ui.spacer(), openOnly),
                needs);
        VBox distributionSection = Ui.section("Distributions",
                Ui.toolbar(impactReport),
                distributions);
        SplitPane bottom = new SplitPane(needSection, distributionSection);
        bottom.setOrientation(Orientation.HORIZONTAL);
        bottom.setDividerPositions(0.57);
        SplitPane tables = new SplitPane(beneficiarySection, bottom);
        tables.setOrientation(Orientation.VERTICAL);
        tables.setDividerPositions(0.45);

        setTop(top);
        setCenter(tables);
        setBottom(status);
        BorderPane.setMargin(tables, new Insets(10, 0, 8, 0));
    }

    private void buildTables() {
        beneficiaries.getColumns().add(Ui.column("ID", 60, Beneficiary::getId));
        beneficiaries.getColumns().add(Ui.column("Name", 130, Beneficiary::getName));
        beneficiaries.getColumns().add(Ui.column("Phone", 110, Beneficiary::getPhone));
        beneficiaries.getColumns().add(Ui.column("E-mail", 140, Beneficiary::getEmail));
        beneficiaries.getColumns().add(Ui.column("Contact by", 85, Beneficiary::getPreferredContact));
        beneficiaries.getColumns().add(Ui.column("Area", 110, Beneficiary::getArea));
        beneficiaries.getColumns().add(Ui.column("Household", 75, Beneficiary::getHouseholdSize));
        beneficiaries.getColumns().add(Ui.column("Open needs", 80, b -> b.openNeeds().size()));
        beneficiaries.getColumns().add(Ui.column("Units received", 95, Beneficiary::unitsReceived));

        needs.getColumns().add(Ui.column("ID", 52, Need::getId));
        needs.getColumns().add(Ui.column("Beneficiary", 98, n -> n.getBeneficiary().getName()));
        needs.getColumns().add(Ui.column("Category", 120, Need::getCategory));
        needs.getColumns().add(Ui.column("Qty", 36, Need::getQuantity));
        needs.getColumns().add(Ui.column("Received", 62, Need::getReceived));
        needs.getColumns().add(Ui.column("Still needed", 78, Need::remaining));
        needs.getColumns().add(urgencyColumn());
        needs.getColumns().add(Ui.column("Requested", 80, Need::getRequestedOn));

        distributions.getColumns().add(Ui.column("Date", 80, Distribution::getDate));
        distributions.getColumns().add(Ui.column("Beneficiary", 98, d -> d.getBeneficiary().getName()));
        distributions.getColumns().add(Ui.column("Given", 140, d -> d.getQuantity() + " x " + d.getCategory()));
        distributions.getColumns().add(Ui.column("Program", 128, Distribution::getProgram));
    }

    /** Urgency as "Low" / "Medium" / "High"; HIGH is drawn as a red badge so urgent needs stand out. */
    private static TableColumn<Need, String> urgencyColumn() {
        TableColumn<Need, String> column = Ui.column("Urgency", 60, n -> urgencyText(n.getUrgency()));
        column.setCellFactory(c -> new TableCell<Need, String>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                setText(null);
                setGraphic(null);
                if (empty || text == null) {
                    return;
                }
                if (text.equals(urgencyText(Urgency.HIGH))) {
                    badge.setText(text);
                    badge.setStyle(URGENT_BADGE);
                    setGraphic(badge);
                } else {
                    setText(text);
                }
            }
        });
        return column;
    }

    private static String urgencyText(Urgency urgency) {
        String name = urgency.name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }

    // ---- What the controller shows ----

    public void showBeneficiaries(List<Beneficiary> rows) {
        Ui.show(beneficiaries, rows, Beneficiary::getId);
        beneficiaries.refresh();
    }

    public void showNeeds(List<Need> rows) {
        Ui.show(needs, rows, Need::getId);
        needs.refresh();
    }

    public void showDistributions(List<Distribution> rows) {
        Ui.show(distributions, rows, Distribution::getId);
        distributions.refresh();
        if (!rows.isEmpty()) {
            distributions.scrollTo(rows.size() - 1); // the newest are what the user just did
        }
    }

    /** @param mode the rule in use (null if it is not one of the menu's rules) */
    public void showRule(AllocationMode mode, String description) {
        showingRule = true;
        rule.setValue(mode);
        showingRule = false;
        ruleName.setText(description);
    }

    /** The short result of the last preview, shown next to the stock buttons. */
    public void showPreview(String text) {
        previewResult.setText(text);
    }

    public void showStatus(String text) {
        status.setText(text);
    }

    // ---- What the user selected ----

    /** @return the selected beneficiary, or null */
    public Beneficiary selectedBeneficiary() {
        return beneficiaries.getSelectionModel().getSelectedItem();
    }

    /** @return the selected need, or null */
    public Need selectedNeed() {
        return needs.getSelectionModel().getSelectedItem();
    }

    /** True when the needs table should list only needs that are not yet met. */
    public boolean openNeedsOnly() {
        return openOnly.isSelected();
    }

    public boolean selectBeneficiary(String id) {
        return Ui.select(beneficiaries, Beneficiary::getId, id);
    }

    public boolean selectNeed(String id) {
        return Ui.select(needs, Need::getId, id);
    }

    // ---- What the user can do (the controller says what each one means) ----

    public void onRegister(Runnable action) { register.setOnAction(e -> action.run()); }
    public void onEdit(Runnable action) { edit.setOnAction(e -> action.run()); }
    public void onRemove(Runnable action) { remove.setOnAction(e -> action.run()); }
    public void onProgressReport(Runnable action) { progressReport.setOnAction(e -> action.run()); }

    public void onAddNeed(Runnable action) { addNeed.setOnAction(e -> action.run()); }
    public void onEditNeed(Runnable action) { editNeed.setOnAction(e -> action.run()); }
    public void onRemoveNeed(Runnable action) { removeNeed.setOnAction(e -> action.run()); }
    public void onGiveDirectly(Runnable action) { giveDirectly.setOnAction(e -> action.run()); }
    public void onOpenNeedsOnly(Runnable action) { openOnly.setOnAction(e -> action.run()); }

    public void onPreview(Runnable action) { preview.setOnAction(e -> action.run()); }
    public void onDistribute(Runnable action) { distribute.setOnAction(e -> action.run()); }
    public void onImpactReport(Runnable action) { impactReport.setOnAction(e -> action.run()); }

    /** Called when the user picks another rule in the drop-down. */
    public void onRuleChanged(Consumer<AllocationMode> action) { ruleListener = action; }
}
