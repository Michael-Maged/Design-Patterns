package org.nonprofit.app;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * MVC pattern (view): everything the user sees in the main console menu.
 * It prints menus, lists and messages and reads what the user types, but
 * makes no decisions - the {@link MainMenuController} does that.
 * Results start with {@code >>} and errors with {@code !!}, like the
 * Volunteers screen. (The Donations screen is the module's own MVC screen.)
 */
public class MainMenuView {
    private final Scanner in;
    private final PrintStream out;

    /** The Scanner is shared with the Donations and Volunteers screens, so all read the same input. */
    public MainMenuView(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    // ---- Menus ----

    public int showMainMenu() {
        return showMenu("NON-PROFIT MANAGEMENT SYSTEM", null, "Exit",
                "Donations", "Volunteers", "Events", "Beneficiaries", "Communication");
    }

    public int showEventMenu() {
        return showMenu("EVENTS", null, "Back",
                "List events", "List upcoming events (by date)", "Create an event",
                "Edit an event (name, date, place, seats, price, type details)",
                "Register an attendee", "Cancel a registration / leave the waitlist", "Check in an attendee",
                "Update an attendee's contact details", "Send a reminder for one event",
                "Send reminders (next 24 hours)", "Attendance report", "Cancel an event", "Delete an event");
    }

    /** The changes "Edit an event" offers, for the event named in {@code title}. */
    public int showEventEditMenu(String title) {
        return showMenu("EDIT " + title, null, "Back",
                "Rename", "Reschedule", "Relocate", "Change capacity", "Change base ticket price",
                "Fundraiser: set the goal", "Fundraiser: record a pledge",
                "Workshop: topic and instructor", "Outreach: target community");
    }

    public int showBeneficiaryMenu(String currentRule) {
        return showMenu("BENEFICIARIES", "Allocation rule: " + currentRule, "Back",
                "List beneficiaries", "Register a beneficiary", "Update a beneficiary", "Remove a beneficiary",
                "Add a need", "Update a need", "Remove a need", "List open needs",
                "Change the allocation rule", "Preview a distribution", "Distribute stock",
                "Give directly to one need", "List distributions", "Impact report");
    }

    public int showCommunicationMenu() {
        return showMenu("COMMUNICATION", null, "Back",
                "Broadcast to a group", "Show the message log");
    }

    /** Numbered options from 1, then option 0 ("Back" or "Exit"). */
    private int showMenu(String title, String status, String zeroLabel, String... options) {
        out.println();
        out.println("========== " + title + " ==========");
        if (status != null) {
            out.println(status);
        }
        for (int i = 0; i < options.length; i++) {
            out.printf("%2d. %s%n", i + 1, options[i]);
        }
        out.println(" 0. " + zeroLabel);
        return askNumber("Choose");
    }

    // ---- Input ----

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

    /** One line per item, or {@code emptyText} when there is nothing to list. */
    public void showList(List<String> lines, String emptyText) {
        if (lines.isEmpty()) {
            out.println(emptyText);
            return;
        }
        for (String line : lines) {
            out.println("  " + line);
        }
    }

    /** For multi-line text such as receipts and reports. */
    public void showText(String text) {
        out.println(text);
    }

    public void showMessage(String message) {
        out.println(">> " + message);
    }

    /** What the Communication module delivered (or skipped), one line each, already indented. */
    public void showDeliveries(List<String> lines) {
        for (String line : lines) {
            out.println(line);
        }
    }

    /** New notes that arrived in an attendee's inbox (the event OBSERVER updates). */
    public void showInbox(String attendeeName, List<String> notes) {
        for (String note : notes) {
            out.println("    [inbox of " + attendeeName + "] " + note);
        }
    }

    public void showError(String message) {
        out.println("!! " + message);
    }
}
