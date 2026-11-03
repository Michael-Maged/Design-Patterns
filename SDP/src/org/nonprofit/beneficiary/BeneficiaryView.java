package org.nonprofit.beneficiary;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * MVC pattern (view): everything the user sees in the Beneficiaries screen.
 * It prints menus, lists and messages and reads what the user types, but
 * makes no decisions - the {@link BeneficiaryController} does that.
 * Results start with {@code >>} and errors with {@code !!}, like the
 * Volunteers screen.
 */
public class BeneficiaryView {
    private final Scanner in;
    private final PrintStream out;

    /** The main menu passes its own Scanner, so all screens read the same input. */
    public BeneficiaryView(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    // ---- Menu and input ----

    /**
     * Shows the Beneficiaries menu and returns the number chosen,
     * -1 if the input was not a number, or 0 if there is no more input.
     *
     * @param zeroLabel "Exit" for a stand-alone screen, "Back" when opened from the main menu
     */
    public int showMenu(String currentRule, String zeroLabel) {
        String[] options = {
            "List beneficiaries", "Register a beneficiary", "Update a beneficiary", "Remove a beneficiary",
            "Add a need", "Update a need", "Remove a need", "List open needs",
            "Change the allocation rule", "Preview a distribution", "Distribute stock",
            "Give directly to one need", "List distributions", "Impact report"};
        out.println();
        out.println("========== BENEFICIARIES ==========");
        out.println("Allocation rule: " + currentRule);
        for (int i = 0; i < options.length; i++) {
            out.printf("%2d. %s%n", i + 1, options[i]);
        }
        out.println(" 0. " + zeroLabel);
        return askNumber("Choose");
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

    /** For multi-line text such as the impact and progress reports. */
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

    public void showError(String message) {
        out.println("!! " + message);
    }
}
