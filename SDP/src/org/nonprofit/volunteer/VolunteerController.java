package org.nonprofit.volunteer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.EventBus;
import org.nonprofit.core.SystemEventType;

/**
 * MVC pattern (controller): takes the user's menu choice from the
 * {@link VolunteerView}, calls the model ({@link VolunteerService} and the
 * classes it manages) and tells the view what to show. The view never
 * talks to the model directly, and the model knows nothing about menus.
 */
public class VolunteerController {
    private final VolunteerService service;
    private final VolunteerView view;

    public VolunteerController(VolunteerService service, VolunteerView view) {
        this.service = service;
        this.view = view;
        // The view shows a notification whenever the model assigns a task.
        EventBus.getInstance().subscribe(SystemEventType.VOLUNTEER_ASSIGNED, view);
    }

    /** Shows the menu again after every action until the user chooses Exit. */
    public void run() {
        boolean running = true;
        while (running) {
            int choice = view.showMenu(service.getStrategy().name());
            try {
                switch (choice) {
                    case 1: view.showVolunteers(service.listVolunteers()); break;
                    case 2: registerVolunteer(); break;
                    case 3: updateProfile(); break;
                    case 4: removeVolunteer(); break;
                    case 5: addTask(); break;
                    case 6: view.showTasks(service.listTasks()); break;
                    case 7: changeRule(); break;
                    case 8: suggest(); break;
                    case 9: assign(); break;
                    case 10: assignAll(); break;
                    case 11: logHours(); break;
                    case 12: view.showText(service.leaderboardReport(5)); break;
                    case 13: hoursCertificate(); break;
                    case 14: participationCertificate(); break;
                    case 0: running = false; break;
                    default: view.showError("Please choose a number from the menu.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                // Rules broken in the model (missing name, task already assigned, ...)
                // are shown to the user instead of stopping the program.
                view.showError(e.getMessage());
            }
        }
        view.showMessage("Goodbye.");
    }

    // ---- Volunteers ----

    private void registerVolunteer() {
        String name = view.ask("Name");
        String email = view.ask("E-mail");
        String phone = view.ask("Phone");
        ContactPreference contact = parseContact(view.ask("Contact by (email / sms / social) [sms]"));
        List<String> skills = splitList(view.ask("Skills, separated by commas"));
        List<DayOfWeek> days = new ArrayList<>();
        for (String day : splitList(view.ask("Available days, separated by commas (e.g. mon, sat)"))) {
            days.add(parseDay(day));
        }
        List<String> categories = splitList(view.ask("Preferred task categories, separated by commas"));

        // Everything typed is valid, so the volunteer can be created now.
        Volunteer volunteer = new Volunteer(name, email, phone, contact);
        for (String skill : skills) {
            volunteer.addSkill(skill);
        }
        for (DayOfWeek day : days) {
            volunteer.addAvailability(day);
        }
        for (String category : categories) {
            volunteer.addPreference(category);
        }
        service.registerVolunteer(volunteer);
        view.showMessage("Registered " + volunteer.summary());
    }

    private void updateProfile() {
        Volunteer volunteer = requireVolunteer(view.ask("Volunteer id (e.g. VOL-1)"));
        int choice = view.showProfileMenu();
        switch (choice) {
            case 1: volunteer.addSkill(view.ask("Skill to add")); break;
            case 2: volunteer.removeSkill(view.ask("Skill to remove")); break;
            case 3: volunteer.addAvailability(parseDay(view.ask("Day to add"))); break;
            case 4: volunteer.removeAvailability(parseDay(view.ask("Day to remove"))); break;
            case 5: volunteer.addPreference(view.ask("Category to add")); break;
            case 6: volunteer.removePreference(view.ask("Category to remove")); break;
            default:
                view.showError("Please choose a number from the menu.");
                return;
        }
        view.showMessage("Updated " + volunteer.summary());
    }

    private void removeVolunteer() {
        String id = view.ask("Volunteer id to remove");
        if (service.removeVolunteer(id)) {
            view.showMessage("Removed " + id + ". Their unfinished tasks are open again.");
        } else {
            view.showError("No volunteer with id " + id);
        }
    }

    // ---- Tasks and assignment ----

    private void addTask() {
        String name = view.ask("Task name");
        String skill = view.ask("Required skill");
        DayOfWeek day = parseDay(view.ask("Day (e.g. sat)"));
        String category = view.ask("Category (e.g. health, logistics)");
        String event = view.ask("Event name (leave empty if none)");
        Task task = new Task(name, skill, day, category, event.isEmpty() ? null : event);
        service.addTask(task);
        view.showMessage("Added " + task.summary());
    }

    private void changeRule() {
        int choice = view.showRuleMenu();
        switch (choice) {
            case 1: service.setStrategy(AssignmentMode.SKILL); break;
            case 2: service.setStrategy(AssignmentMode.AVAILABILITY); break;
            case 3: service.setStrategy(AssignmentMode.PREFERENCE); break;
            default:
                view.showError("Please choose 1, 2 or 3.");
                return;
        }
        view.showMessage("Assignment rule is now: " + service.getStrategy().name());
    }

    private void suggest() {
        Task task = requireTask(view.ask("Task id (e.g. TSK-1)"));
        Volunteer volunteer = service.suggest(task.getId());
        if (volunteer == null) {
            view.showMessage("Nobody fits " + task.getName() + " with the current rule.");
        } else {
            view.showMessage("The current rule would choose " + volunteer.getName() + " for " + task.getName() + ".");
        }
    }

    private void assign() {
        Task task = requireTask(view.ask("Task id to assign"));
        Volunteer volunteer = service.assign(task.getId());
        if (volunteer == null) {
            view.showMessage("Nobody fits " + task.getName() + " with the current rule; it stays open.");
        } else {
            view.showMessage(task.getName() + " assigned to " + volunteer.getName() + ".");
        }
    }

    private void assignAll() {
        int filled = service.assignAllOpen();
        view.showMessage(filled + " task(s) assigned; " + service.openTasks().size() + " still open.");
    }

    private void logHours() {
        Task task = requireTask(view.ask("Task id"));
        double hours = parseHours(view.ask("Hours worked"));
        service.logHours(task.getId(), hours, LocalDate.now());
        view.showMessage("Logged " + hours + " h for " + task.getName() + ".");
    }

    // ---- Certificates ----

    private void hoursCertificate() {
        String id = view.ask("Volunteer id");
        view.showText(service.issueHoursCertificate(id, LocalDate.now()));
    }

    private void participationCertificate() {
        String id = view.ask("Volunteer id");
        String event = view.ask("Event name");
        view.showText(service.issueParticipationCertificate(id, event, LocalDate.now()));
    }

    // ---- Turning what the user typed into values ----

    private Volunteer requireVolunteer(String id) {
        Volunteer volunteer = service.findVolunteer(id);
        if (volunteer == null) {
            throw new IllegalArgumentException("No volunteer with id " + id);
        }
        return volunteer;
    }

    private Task requireTask(String id) {
        Task task = service.findTask(id);
        if (task == null) {
            throw new IllegalArgumentException("No task with id " + id);
        }
        return task;
    }

    /** Accepts full names or the first three letters: "saturday", "Sat". */
    private static DayOfWeek parseDay(String text) {
        String key = text.trim().toUpperCase(Locale.ROOT);
        if (key.length() >= 3) {
            for (DayOfWeek day : DayOfWeek.values()) {
                if (day.name().startsWith(key)) {
                    return day;
                }
            }
        }
        throw new IllegalArgumentException("Unknown day: " + text + " (use mon, tue, wed, thu, fri, sat or sun)");
    }

    private static ContactPreference parseContact(String text) {
        String key = text.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty() || key.equals("sms")) {
            return ContactPreference.SMS;
        }
        if (key.equals("email") || key.equals("e-mail")) {
            return ContactPreference.EMAIL;
        }
        if (key.equals("social") || key.equals("social media")) {
            return ContactPreference.SOCIAL_MEDIA;
        }
        throw new IllegalArgumentException("Unknown contact method: " + text);
    }

    private static double parseHours(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Hours must be a number, e.g. 2.5");
        }
    }

    /** "first aid, driving" becomes ["first aid", "driving"]; empty parts are skipped. */
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
