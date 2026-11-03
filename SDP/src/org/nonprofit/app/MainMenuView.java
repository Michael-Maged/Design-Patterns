package org.nonprofit.app;

import java.io.PrintStream;
import java.util.Scanner;

/**
 * MVC pattern (view): the main console menu. It prints the menu and
 * messages and reads the user's choice, but makes no decisions - the
 * {@link MainMenuController} does that. Each module's screen has its own view.
 * Results start with {@code >>} and errors with {@code !!}, like the module screens.
 */
public class MainMenuView {
    private final Scanner in;
    private final PrintStream out;

    /** The Scanner is shared with the five module screens, so all read the same input. */
    public MainMenuView(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    /** Returns the number chosen, -1 if the answer is not a number and 0 if there is no more input. */
    public int showMainMenu() {
        String[] options = {"Donations", "Volunteers", "Events", "Beneficiaries", "Communication"};
        out.println();
        out.println("========== NON-PROFIT MANAGEMENT SYSTEM ==========");
        for (int i = 0; i < options.length; i++) {
            out.printf("%2d. %s%n", i + 1, options[i]);
        }
        out.println(" 0. Exit");
        out.print("Choose: ");
        if (!in.hasNextLine()) {
            return 0;
        }
        try {
            return Integer.parseInt(in.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public void showMessage(String message) {
        out.println(">> " + message);
    }

    public void showError(String message) {
        out.println("!! " + message);
    }
}
