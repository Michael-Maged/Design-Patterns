package org.nonprofit.gui.volunteer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import org.nonprofit.core.model.ContactPreference;
import org.nonprofit.gui.common.Dialogs;
import org.nonprofit.gui.common.Feed;
import org.nonprofit.gui.common.Form;
import org.nonprofit.gui.common.Inputs;
import org.nonprofit.gui.common.ScreenActions;
import org.nonprofit.volunteer.strategy.AssignmentMode;
import org.nonprofit.volunteer.model.Task;
import org.nonprofit.volunteer.model.Volunteer;
import org.nonprofit.volunteer.model.VolunteerService;

/**
 * MVC pattern (controller) of the Volunteers screen in the GUI: turns each
 * click on the {@link VolunteerPanel} into calls on the model
 * ({@link VolunteerService}, unchanged from the console version) and tells
 * the panel what to show. Rules broken in the model come back as
 * exceptions and are shown as error dialogs by {@link ScreenActions}.
 *
 * The rule drop-down swaps the service's STRATEGY at runtime; the
 * certificates come from the module's TEMPLATE METHOD.
 */
public class VolunteerGuiController {
    private static final int LEADERBOARD_SIZE = 10;

    private final VolunteerService service;
    private final VolunteerPanel panel;
    private final Dialogs dialogs;
    private final ScreenActions actions;

    /**
     * @param deliveries the messages Communication sent since the last call
     *                   ({@code CommunicationService::takeOutput}); this module does not import Communication
     */
    public VolunteerGuiController(VolunteerService service, Dialogs dialogs, Feed feed,
                                  Supplier<List<String>> deliveries) {
        this.service = service;
        this.dialogs = dialogs;
        this.panel = new VolunteerPanel(List.of(AssignmentMode.values()));
        this.actions = new ScreenActions(dialogs, feed, deliveries, this::refresh);

        panel.onRegister(() -> actions.run(this::registerVolunteer));
        panel.onEditProfile(() -> actions.run(this::editProfile));
        panel.onRemoveVolunteer(() -> actions.run(this::removeVolunteer));
        panel.onLeaderboard(() -> actions.run(this::leaderboard));
        panel.onHoursCertificate(() -> actions.run(this::hoursCertificate));
        panel.onParticipationCertificate(() -> actions.run(this::participationCertificate));

        panel.onAddTask(() -> actions.run(this::addTask));
        panel.onEditTask(() -> actions.run(this::editTask));
        panel.onRemoveTask(() -> actions.run(this::removeTask));
        panel.onAssign(() -> actions.run(this::assign));
        panel.onAssignAll(() -> actions.run(this::assignAll));
        panel.onUnassign(() -> actions.run(this::unassign));
        panel.onLogHours(() -> actions.run(this::logHours));

        panel.onRuleChanged(mode -> actions.run(() -> changeRule(mode)));
        panel.onSuggest(() -> actions.run(this::suggest));
        panel.onTaskSelected(this::showSuggestion);
        refresh();
    }

    public VolunteerPanel getPanel() {
        return panel;
    }

    /** Reloads both tables and the rule bar from the service. */
    public void refresh() {
        panel.showVolunteers(service.listVolunteers());
        panel.showTasks(service.listTasks());
        panel.showRule(currentMode(), service.getStrategy().name());
        showSuggestion();
    }

    // ---- Volunteers ----

    private void registerVolunteer() {
        Form form = new Form("Register a volunteer")
                .text("name", "Name", "")
                .text("email", "E-mail", "")
                .text("phone", "Phone", "")
                .choice("contact", "Contact by", List.of(ContactPreference.values()), ContactPreference.SMS)
                .text("skills", "Skills", "", "first aid, driving")
                .checks("days", "Available days", List.of(DayOfWeek.values()), List.of())
                .text("preferences", "Preferred categories", "", "health, logistics");
        dialogs.form(form).ifPresent(values -> {
            // Everything is read first; the Volunteer constructor refuses a blank name.
            Volunteer volunteer = new Volunteer(values.text("name"), Inputs.orNull(values.text("email")),
                    Inputs.orNull(values.text("phone")), values.choice("contact", ContactPreference.class));
            for (String skill : Inputs.list(values.text("skills"))) {
                volunteer.addSkill(skill);
            }
            for (DayOfWeek day : values.checked("days", DayOfWeek.class)) {
                volunteer.addAvailability(day);
            }
            for (String category : Inputs.list(values.text("preferences"))) {
                volunteer.addPreference(category);
            }
            service.registerVolunteer(volunteer);
            refresh(); // so the new row is in the table and can be selected
            panel.selectVolunteer(volunteer.getId());
            panel.showStatus("Registered " + volunteer.getName() + " as " + volunteer.getId() + ".");
        });
    }

    /**
     * One form for contact details, skills, days and preferences. The changes
     * go through the service one by one (contact first, then removals, then
     * additions), so its rules apply, e.g. a day with an unfinished task
     * cannot be removed. If the service refuses one, the old profile is put
     * back, so the edit is all or nothing.
     */
    private void editProfile() {
        Volunteer v = requireSelectedVolunteer();
        Form form = new Form("Edit " + v.getName() + " (" + v.getId() + ")")
                .text("name", "Name", v.getName())
                .text("email", "E-mail", v.getEmail())
                .text("phone", "Phone", v.getPhone())
                .choice("contact", "Contact by", List.of(ContactPreference.values()), v.getPreferredContact())
                .text("skills", "Skills", String.join(", ", v.getSkills()), "first aid, driving")
                .checks("days", "Available days", List.of(DayOfWeek.values()), v.getAvailability())
                .text("preferences", "Preferred categories", String.join(", ", v.getPreferredCategories()),
                        "health, logistics");
        dialogs.form(form).ifPresent(values -> {
            // Kept so a change the service refuses half-way can be undone (all or nothing).
            String oldName = v.getName();
            String oldEmail = v.getEmail();
            String oldPhone = v.getPhone();
            ContactPreference oldContact = v.getPreferredContact();
            Set<String> oldSkills = v.getSkills();
            Set<DayOfWeek> oldDays = v.getAvailability();
            Set<String> oldPreferences = v.getPreferredCategories();
            try {
                applyProfile(v.getId(), values.text("name"), Inputs.orNull(values.text("email")),
                        Inputs.orNull(values.text("phone")), values.choice("contact", ContactPreference.class),
                        Inputs.list(values.text("skills")), values.checked("days", DayOfWeek.class),
                        Inputs.list(values.text("preferences")));
            } catch (IllegalArgumentException | IllegalStateException e) {
                // Removals are the only steps that can be refused; adding the old values back is always allowed.
                service.updateVolunteerContact(v.getId(), oldName, oldEmail, oldPhone, oldContact);
                applyProfile(v.getId(), oldName, oldEmail, oldPhone, oldContact,
                        new ArrayList<>(oldSkills), new ArrayList<>(oldDays), new ArrayList<>(oldPreferences));
                throw new IllegalStateException(e.getMessage() + ". Nothing was changed.");
            }
            panel.showStatus("Updated the profile of " + v.getName() + ".");
        });
    }

    /** Contact details first, then the removals (which the service may refuse), then the additions. */
    private void applyProfile(String id, String name, String email, String phone, ContactPreference contact,
                              List<String> skills, List<DayOfWeek> days, List<String> preferences) {
        Volunteer v = service.updateVolunteerContact(id, name, email, phone, contact);
        for (String skill : v.getSkills()) {
            if (!containsIgnoringCase(skills, skill)) {
                service.removeSkill(id, skill);
            }
        }
        for (DayOfWeek day : v.getAvailability()) {
            if (!days.contains(day)) {
                service.removeAvailability(id, day);
            }
        }
        for (String category : v.getPreferredCategories()) {
            if (!containsIgnoringCase(preferences, category)) {
                service.removePreference(id, category);
            }
        }
        for (String skill : skills) {
            service.addSkill(id, skill);
        }
        for (DayOfWeek day : days) {
            service.addAvailability(id, day);
        }
        for (String category : preferences) {
            service.addPreference(id, category);
        }
    }

    private void removeVolunteer() {
        Volunteer v = requireSelectedVolunteer();
        if (dialogs.confirm("Remove " + v.getName() + " (" + v.getId() + ")? Their unfinished tasks become open again.")
                && service.removeVolunteer(v.getId())) {
            panel.showStatus("Removed " + v.getName() + ". Their unfinished tasks are open again.");
        }
    }

    private void leaderboard() {
        dialogs.text("Leaderboard", service.leaderboardReport(LEADERBOARD_SIZE));
    }

    // ---- Certificates (Template Method in the model) ----

    private void hoursCertificate() {
        Volunteer v = requireSelectedVolunteer();
        dialogs.text("Hours certificate - " + v.getName(), service.issueHoursCertificate(v.getId(), LocalDate.now()));
    }

    private void participationCertificate() {
        Volunteer v = requireSelectedVolunteer();
        List<String> events = eventNames();
        if (events.isEmpty()) {
            throw new IllegalStateException("No task belongs to an event yet");
        }
        Form form = new Form("Event participation certificate")
                .header("For " + v.getName() + " (" + v.getId() + ")")
                .choice("event", "Event", events, events.get(0))
                .date("date", "Issue date", LocalDate.now());
        dialogs.form(form).ifPresent(values -> {
            String event = values.choice("event", String.class);
            if (event == null) {
                throw new IllegalArgumentException("Event is required");
            }
            LocalDate date = values.date("date") == null ? LocalDate.now() : values.date("date");
            dialogs.text("Participation certificate - " + v.getName(),
                    service.issueParticipationCertificate(v.getId(), event, date));
        });
    }

    /** The event names that appear on tasks, each once. */
    private List<String> eventNames() {
        List<String> names = new ArrayList<>();
        for (Task task : service.listTasks()) {
            String event = task.getEventName();
            if (event != null && !containsIgnoringCase(names, event)) {
                names.add(event);
            }
        }
        return names;
    }

    // ---- Tasks ----

    private void addTask() {
        Form form = taskForm("Add a task", "", "", null, "", "");
        dialogs.form(form).ifPresent(values -> {
            Task task = service.addTask(new Task(values.text("name"), values.text("skill"),
                    values.choice("day", DayOfWeek.class), values.text("category"),
                    Inputs.orNull(values.text("event"))));
            refresh();
            panel.selectTask(task.getId());
            panel.showStatus("Added " + task.getName() + " (" + task.getId() + ").");
        });
    }

    private void editTask() {
        Task task = requireSelectedTask();
        Form form = taskForm("Edit " + task.getName() + " (" + task.getId() + ")", task.getName(),
                task.getRequiredSkill(), task.getDay(), task.getCategory(), task.getEventName());
        dialogs.form(form).ifPresent(values -> {
            service.updateTask(task.getId(), values.text("name"), values.text("skill"),
                    values.choice("day", DayOfWeek.class), values.text("category"),
                    Inputs.orNull(values.text("event")));
            panel.showStatus("Updated " + task.getName() + ".");
        });
    }

    private static Form taskForm(String title, String name, String skill, DayOfWeek day, String category,
                                 String event) {
        return new Form(title)
                .text("name", "Task name", name)
                .text("skill", "Required skill", skill, "first aid")
                .choice("day", "Day", List.of(DayOfWeek.values()), day)
                .text("category", "Category", category, "health, logistics, food")
                .text("event", "Event (optional)", event);
    }

    private void removeTask() {
        Task task = requireSelectedTask();
        if (dialogs.confirm("Remove " + task.getName() + " (" + task.getId() + ")?")) {
            service.removeTask(task.getId());
            panel.showStatus("Removed " + task.getName() + ".");
        }
    }

    // ---- Assignment (Strategy in the model) ----

    private void changeRule(AssignmentMode mode) {
        service.setStrategy(mode);
        panel.showStatus("Assignment rule is now: " + service.getStrategy().name() + ".");
    }

    private void suggest() {
        Task task = requireSelectedTask();
        showSuggestion();
        panel.showStatus(suggestionFor(task));
    }

    /** Shows who the current rule would choose for the selected task, without assigning anyone. */
    private void showSuggestion() {
        Task task = panel.selectedTask();
        panel.showSuggestion(task == null ? "Select a task to see who the current rule picks." : suggestionFor(task));
    }

    private String suggestionFor(Task task) {
        if (task.isCompleted()) {
            return task.getName() + " is done.";
        }
        if (task.isAssigned()) {
            return task.getName() + " is already assigned to " + task.getAssignedVolunteer().getName() + ".";
        }
        Volunteer pick = service.suggest(task.getId());
        return pick == null
                ? "Nobody fits " + task.getName() + " with the current rule."
                : "The current rule picks " + pick.getName() + " for " + task.getName() + ".";
    }

    private void assign() {
        Task task = requireSelectedTask();
        Volunteer volunteer = service.assign(task.getId());
        panel.showStatus(volunteer == null
                ? "Nobody fits " + task.getName() + " with the current rule; it stays open."
                : task.getName() + " assigned to " + volunteer.getName() + ".");
    }

    private void assignAll() {
        int filled = service.assignAllOpen();
        panel.showStatus(filled + " task(s) assigned; " + service.openTasks().size() + " still open.");
    }

    private void unassign() {
        Task task = requireSelectedTask();
        Volunteer volunteer = task.getAssignedVolunteer();
        if (volunteer == null) {
            panel.showStatus(task.getName() + " has no volunteer.");
            return;
        }
        service.unassign(task.getId());
        panel.showStatus(volunteer.getName() + " is no longer assigned to " + task.getName() + ".");
    }

    private void logHours() {
        Task task = requireSelectedTask();
        Form form = new Form("Log hours")
                .header(task.getName() + " (" + task.getId() + ")")
                .text("hours", "Hours worked", "", "2.5")
                .date("date", "Date", LocalDate.now());
        dialogs.form(form).ifPresent(values -> {
            double hours = Inputs.number(values.text("hours"), "Hours");
            service.logHours(task.getId(), hours, values.date("date"));
            panel.showStatus("Logged " + hours + " h for " + task.getName() + ".");
        });
    }

    // ---- Helpers ----

    /** The menu entry for the strategy in use, or null if it is none of them. */
    private AssignmentMode currentMode() {
        for (AssignmentMode mode : AssignmentMode.values()) {
            if (mode.create().getClass() == service.getStrategy().getClass()) {
                return mode;
            }
        }
        return null;
    }

    private Volunteer requireSelectedVolunteer() {
        Volunteer volunteer = panel.selectedVolunteer();
        if (volunteer == null) {
            throw new IllegalArgumentException("Select a volunteer in the table first.");
        }
        return volunteer;
    }

    private Task requireSelectedTask() {
        Task task = panel.selectedTask();
        if (task == null) {
            throw new IllegalArgumentException("Select a task in the table first.");
        }
        return task;
    }

    private static boolean containsIgnoringCase(List<String> list, String text) {
        for (String item : list) {
            if (item.trim().toLowerCase(Locale.ROOT).equals(text.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
