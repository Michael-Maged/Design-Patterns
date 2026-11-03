package org.nonprofit.donation;

import java.util.List;
import java.util.Scanner;

import org.nonprofit.core.SystemEvent;
import org.nonprofit.core.SystemObserver;

/**
 * MVC pattern (VIEW): only prints things and reads what the user types.
 * It never decides anything - that is the controller's job.
 *
 * It is also an OBSERVER: it listens to the EventBus and shows a small
 * notification every time a donation is received.
 */
public class DonationView implements SystemObserver {
    private final Scanner in = new Scanner(System.in);

    public int showMenu() {
        System.out.println();
        System.out.println("=========== DONATIONS ===========");
        System.out.println(" 1. List donors");
        System.out.println(" 2. Add a donor");
        System.out.println(" 3. Remove a donor");
        System.out.println(" 4. Add a campaign");
        System.out.println(" 5. List campaigns");
        System.out.println(" 6. Make a donation");
        System.out.println(" 7. List donations");
        System.out.println(" 8. Print a receipt");
        System.out.println(" 9. Print a tax statement");
        System.out.println("10. Summary and top donors");
        System.out.println(" 0. Exit");
        return askNumber("Choose");
    }

    /** Asks a question and returns the answer. Returns "" when there is no more input. */
    public String ask(String question) {
        System.out.print(question + ": ");
        if (!in.hasNextLine()) {
            return "";
        }
        return in.nextLine().trim();
    }

    /** Returns -1 if the user typed something that isn't a number, 0 if input ended. */
    public int askNumber(String question) {
        String answer = ask(question);
        if (answer.isEmpty() && !in.hasNextLine()) {
            return 0;
        }
        try {
            return Integer.parseInt(answer);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void showError(String message) {
        System.out.println("Error: " + message);
    }

    /** Prints any list (donors, donations, campaigns), or a message if it's empty. */
    public void showList(String title, List<?> items) {
        System.out.println("--- " + title + " ---");
        if (items.isEmpty()) {
            System.out.println("(nothing yet)");
        }
        for (Object item : items) {
            System.out.println("  " + item);
        }
    }

    /** Called by the EventBus when a donation is received. */
    @Override
    public void onEvent(SystemEvent event) {
        System.out.println("  [notification] " + event.getRecipient().getName() + ": " + event.getMessage());
    }
}
