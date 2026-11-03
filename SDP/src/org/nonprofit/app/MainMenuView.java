package org.nonprofit.app;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * MVC pattern (view): everything the user sees in the main console menu.
 * It prints menus, lists and messages and reads what the user types, but
 * makes no decisions - the {@link MainMenuController} does that.
 * Results start with {@code >>} and errors with {@code !!}, like the
 * Volunteers screen.
 */
public class MainMenuView {
    private final Scanner in;
    private final PrintStream out;

    /** The Scanner is shared with the Volunteers screen, so both read the same input. */
    public MainMenuView(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    // ---- Menus ----

    public int showMainMenu() {
        return showMenu("NON-PROFIT MANAGEMENT SYSTEM", null, "Exit",
                "Donations", "Volunteers", "Events", "Beneficiaries", "Communication");
    }

    public int showDonationMenu() {
        return showMenu("DONATIONS", null, "Back",
                "List donors", "Register a donor", "Record a donation", "List donations",
                "Print a receipt", "Print a tax statement", "Totals, campaigns and top donors");
    }

    public int showEventMenu() {
        return showMenu("EVENTS", null, "Back",
                "List events", "Create an event", "Register an attendee", "Check in an attendee",
                "Cancel an event", "Send reminders (next 24 hours)", "Attendance report");
    }

    public int showBeneficiaryMenu(String currentRule) {
        return showMenu("BENEFICIARIES", "Allocation rule: " + currentRule, "Back",
                "List beneficiaries", "Register a beneficiary", "Add a need", "List open needs",
                "Change the allocation rule", "Distribute stock", "Impact report");
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

    public void showError(String message) {
        out.println("!! " + message);
    }
}
