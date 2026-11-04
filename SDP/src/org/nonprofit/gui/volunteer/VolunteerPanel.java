package org.nonprofit.gui.volunteer;

import java.util.List;
import java.util.function.Consumer;

import org.nonprofit.gui.common.Ui;
import org.nonprofit.volunteer.strategy.AssignmentMode;
import org.nonprofit.volunteer.model.Task;
import org.nonprofit.volunteer.model.Volunteer;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * MVC pattern (view) of the Volunteers screen in the GUI: the volunteers and
 * tasks tables, their buttons and the assignment-rule bar. It only shows
 * what it is given and reports clicks; {@link VolunteerGuiController}
 * decides what happens.
 */
public class VolunteerPanel extends BorderPane {
    private final TableView<Volunteer> volunteers = Ui.table("volunteers-table", "No volunteers yet.");
    private final TableView<Task> tasks = Ui.table("volunteers-task-table", "No tasks yet.");

    private final Button register = Ui.button("Register...", "volunteers-register");
    private final Button editProfile = Ui.button("Edit profile...", "volunteers-edit");
    private final Button removeVolunteer = Ui.button("Remove", "volunteers-remove");
    private final Button leaderboard = Ui.button("Leaderboard", "volunteers-leaderboard");
    private final Button hoursCertificate = Ui.button("Hours certificate", "volunteers-hours-certificate");
    private final Button participationCertificate =
            Ui.button("Event participation certificate...", "volunteers-participation-certificate");

    private final Button addTask = Ui.button("Add task...", "volunteers-task-add");
    private final Button editTask = Ui.button("Edit...", "volunteers-task-edit");
    private final Button removeTask = Ui.button("Remove", "volunteers-task-remove");
    private final Button assign = Ui.button("Assign", "volunteers-task-assign");
    private final Button assignAll = Ui.button("Assign all open", "volunteers-task-assign-all");
    private final Button unassign = Ui.button("Unassign", "volunteers-task-unassign");
    private final Button logHours = Ui.button("Log hours...", "volunteers-task-log-hours");

    private final ComboBox<AssignmentMode> rule = new ComboBox<>();
    private final Label ruleName = new Label();
    private final Button suggest = Ui.button("Suggest", "volunteers-suggest");
    private final Label suggestion = new Label();
    private final Label status = Ui.statusBar("volunteers-status");

    /** True while the controller sets the rule, so that is not reported as the user's choice. */
    private boolean showingRule;
    private Consumer<AssignmentMode> ruleListener = mode -> { };

    public VolunteerPanel(List<AssignmentMode> rules) {
        setId("volunteers-screen");
        getStyleClass().add("screen");
        buildTables();

        rule.setId("volunteers-rule");
        rule.getItems().setAll(rules);
        rule.setConverter(new StringConverter<AssignmentMode>() {
            @Override
            public String toString(AssignmentMode mode) {
                return Ui.display(mode);
            }

            @Override
            public AssignmentMode fromString(String text) {
                return null;
            }
        });
        rule.valueProperty().addListener((property, old, chosen) -> {
            if (!showingRule && chosen != null) {
                ruleListener.accept(chosen);
            }
        });
        ruleName.setId("volunteers-rule-name");
        ruleName.getStyleClass().add("muted");
        suggestion.setId("volunteers-suggestion");
        suggestion.getStyleClass().add("highlight");

        Label ruleTitle = new Label("Assignment rule (Strategy):");
        ruleTitle.getStyleClass().add("info-bar-title");
        HBox ruleRow = new HBox(10, ruleTitle, rule, ruleName);
        ruleRow.setAlignment(Pos.CENTER_LEFT);
        Label pickTitle = new Label("For the selected task:");
        HBox pickRow = new HBox(10, pickTitle, suggest, suggestion);
        pickRow.setAlignment(Pos.CENTER_LEFT);
        VBox ruleBar = new VBox(6, ruleRow, pickRow);
        ruleBar.getStyleClass().add("info-bar");

        VBox top = new VBox(10, Ui.header("Volunteers",
                "Profiles, tasks, assignment and hours. Pick a rule to change who the system chooses for a task."),
                ruleBar);

        VBox volunteerSection = Ui.section("Volunteers",
                Ui.toolbar(register, editProfile, removeVolunteer, Ui.gap(),
                        leaderboard, hoursCertificate, participationCertificate),
                volunteers);
        VBox taskSection = Ui.section("Tasks",
                Ui.toolbar(addTask, editTask, removeTask, Ui.gap(), assign, assignAll, unassign, Ui.gap(), logHours),
                tasks);
        SplitPane tables = new SplitPane(volunteerSection, taskSection);
        tables.setOrientation(Orientation.VERTICAL);
        tables.setDividerPositions(0.5);

        setTop(top);
        setCenter(tables);
        setBottom(status);
        BorderPane.setMargin(tables, new Insets(10, 0, 8, 0));
    }

    private void buildTables() {
        volunteers.getColumns().add(Ui.column("ID", 60, Volunteer::getId));
        volunteers.getColumns().add(Ui.column("Name", 120, Volunteer::getName));
        volunteers.getColumns().add(Ui.column("E-mail", 140, Volunteer::getEmail));
        volunteers.getColumns().add(Ui.column("Phone", 110, Volunteer::getPhone));
        volunteers.getColumns().add(Ui.column("Contact by", 80, Volunteer::getPreferredContact));
        volunteers.getColumns().add(Ui.column("Skills", 130, Volunteer::getSkills));
        volunteers.getColumns().add(Ui.column("Days", 90, v -> Ui.days(v.getAvailability())));
        volunteers.getColumns().add(Ui.column("Preferences", 100, Volunteer::getPreferredCategories));
        volunteers.getColumns().add(Ui.column("Hours", 55, Volunteer::getTotalHours));
        volunteers.getColumns().add(Ui.column("Open tasks", 75, Volunteer::openTaskCount));

        tasks.getColumns().add(Ui.column("ID", 60, Task::getId));
        tasks.getColumns().add(Ui.column("Task", 130, Task::getName));
        tasks.getColumns().add(Ui.column("Skill", 90, Task::getRequiredSkill));
        tasks.getColumns().add(Ui.column("Day", 80, Task::getDay));
        tasks.getColumns().add(Ui.column("Category", 85, Task::getCategory));
        tasks.getColumns().add(Ui.column("Event", 170, Task::getEventName));
        tasks.getColumns().add(Ui.column("Volunteer", 120,
                t -> t.getAssignedVolunteer() == null ? "" : t.getAssignedVolunteer().getName()));
        tasks.getColumns().add(Ui.column("Status", 80, VolunteerPanel::statusOf));
    }

    private static String statusOf(Task task) {
        if (task.isCompleted()) {
            return "Done";
        }
        return task.isAssigned() ? "Assigned" : "Open";
    }

    // ---- What the controller shows ----

    public void showVolunteers(List<Volunteer> rows) {
        Ui.show(volunteers, rows, Volunteer::getId);
        volunteers.refresh();
    }

    public void showTasks(List<Task> rows) {
        Ui.show(tasks, rows, Task::getId);
        tasks.refresh();
    }

    /** @param mode the rule in use (null if it is not one of the menu's rules) */
    public void showRule(AssignmentMode mode, String description) {
        showingRule = true;
        rule.setValue(mode);
        showingRule = false;
        ruleName.setText(description);
    }

    public void showSuggestion(String text) {
        suggestion.setText(text);
    }

    public void showStatus(String text) {
        status.setText(text);
    }

    // ---- What the user selected ----

    /** @return the selected volunteer, or null */
    public Volunteer selectedVolunteer() {
        return volunteers.getSelectionModel().getSelectedItem();
    }

    /** @return the selected task, or null */
    public Task selectedTask() {
        return tasks.getSelectionModel().getSelectedItem();
    }

    public boolean selectVolunteer(String id) {
        return Ui.select(volunteers, Volunteer::getId, id);
    }

    public boolean selectTask(String id) {
        return Ui.select(tasks, Task::getId, id);
    }

    // ---- What the user can do (the controller says what each one means) ----

    public void onRegister(Runnable action) { register.setOnAction(e -> action.run()); }
    public void onEditProfile(Runnable action) { editProfile.setOnAction(e -> action.run()); }
    public void onRemoveVolunteer(Runnable action) { removeVolunteer.setOnAction(e -> action.run()); }
    public void onLeaderboard(Runnable action) { leaderboard.setOnAction(e -> action.run()); }
    public void onHoursCertificate(Runnable action) { hoursCertificate.setOnAction(e -> action.run()); }
    public void onParticipationCertificate(Runnable action) {
        participationCertificate.setOnAction(e -> action.run());
    }

    public void onAddTask(Runnable action) { addTask.setOnAction(e -> action.run()); }
    public void onEditTask(Runnable action) { editTask.setOnAction(e -> action.run()); }
    public void onRemoveTask(Runnable action) { removeTask.setOnAction(e -> action.run()); }
    public void onAssign(Runnable action) { assign.setOnAction(e -> action.run()); }
    public void onAssignAll(Runnable action) { assignAll.setOnAction(e -> action.run()); }
    public void onUnassign(Runnable action) { unassign.setOnAction(e -> action.run()); }
    public void onLogHours(Runnable action) { logHours.setOnAction(e -> action.run()); }

    public void onSuggest(Runnable action) { suggest.setOnAction(e -> action.run()); }

    /** Called when the user picks another rule in the drop-down. */
    public void onRuleChanged(Consumer<AssignmentMode> action) { ruleListener = action; }

    /** Called whenever another task is selected (or none). */
    public void onTaskSelected(Runnable action) {
        tasks.getSelectionModel().selectedItemProperty().addListener((property, old, now) -> action.run());
    }
}
