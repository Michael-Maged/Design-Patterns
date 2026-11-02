package org.nonprofit.volunteer;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import org.nonprofit.core.SystemEvent;
import org.nonprofit.core.SystemObserver;

/**
 * MVC pattern (view): everything the user sees in the Volunteers screen.
 * It prints menus, lists and messages and reads what the user types, but
 * makes no decisions - the {@link VolunteerController} does that.
 *
 * It is also an OBSERVER of the EventBus: when the model assigns a task, the
 * view is told and shows a notification, the same way the views in the
 * textbook's MVC chapter observe the model.
 */
public class VolunteerView implements SystemObserver {
    private final Scanner in;
    private final PrintStream out;

    public VolunteerView(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    /** Reads from the keyboard and prints to the console. */
    public VolunteerView() {
        this(new Scanner(System.in), System.out);
    }

    // ---- Menus and input ----

    /**
     * Shows the main Volunteers menu and returns the number chosen,
     * -1 if the input was not a number, or 0 if there is no more input.
     */
    public int showMenu(String currentRule) {
        out.println();
        out.println("========== VOLUNTEERS ==========");
        out.println("Assignment rule: " + currentRule);
        out.println(" 1. List volunteers");
        out.println(" 2. Register a volunteer");
        out.println(" 3. Update a volunteer's profile");
        out.println(" 4. Remove a volunteer");
        out.println(" 5. Add a task");
        out.println(" 6. List tasks");
        out.println(" 7. Remove a task");
        out.println(" 8. Change the assignment rule");
        out.println(" 9. Suggest a volunteer for a task");
        out.println("10. Assign a task");
        out.println("11. Assign all open tasks");
        out.println("12. Unassign a task");
        out.println("13. Log hours for a task");
        out.println("14. Show the leaderboard");
        out.println("15. Issue an hours certificate");
        out.println("16. Issue an event participation certificate");
        out.println(" 0. Exit");
        return askNumber("Choose");
    }

    /** Shows the profile sub-menu and returns the number chosen. */
    public int showProfileMenu() {
        out.println(" 1. Add a skill        2. Remove a skill");
        out.println(" 3. Add a day          4. Remove a day");
        out.println(" 5. Add a preference   6. Remove a preference");
        return askNumber("What do you want to change");
    }

    /** Shows the assignment rules and returns the number chosen. */
    public int showRuleMenu() {
        out.println(" 1. Skill-based");
        out.println(" 2. Availability-based");
        out.println(" 3. Preference-based");
        return askNumber("Choose a rule");
    }

    /** Asks a question and returns the answer without surrounding spaces ("" if there is no more input). */
    public String ask(String prompt) {
        out.print(prompt + ": ");
        if (!in.hasNextLine()) {
            return "";
        }
        return in.nextLine().trim();
    }

    /** Asks for a whole number; returns -1 if the answer is not a number and 0 if there is no more input. */
    public int askNumber(String prompt) {
        out.print(prompt + ": ");
        if (!in.hasNextLine()) {
            return 0;
        }
        String answer = in.nextLine().trim();
        try {
            return Integer.parseInt(answer);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ---- Output ----

    public void showVolunteers(List<Volunteer> volunteers) {
        if (volunteers.isEmpty()) {
            out.println("No volunteers yet.");
            return;
        }
        for (Volunteer v : volunteers) {
            out.println("  " + v.summary());
        }
    }

    public void showTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            out.println("No tasks yet.");
            return;
        }
        for (Task t : tasks) {
            out.println("  " + t.summary());
        }
    }

    /** For multi-line text such as certificates and the leaderboard. */
    public void showText(String text) {
        out.println(text);
    }

    public void showMessage(String message) {
        out.println(">> " + message);
    }

    public void showError(String message) {
        out.println("!! " + message);
    }

    /** Called by the EventBus when the model publishes something the view subscribed to. */
    @Override
    public void onEvent(SystemEvent event) {
        String to = event.getRecipient() == null ? "everyone" : event.getRecipient().getName();
        out.println("   [Notification to " + to + "] " + event.getMessage());
    }
}
