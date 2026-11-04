package org.nonprofit.communication.mvc;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;
import org.nonprofit.communication.model.Message;
import org.nonprofit.communication.model.SentMessage;

/**
 * MVC pattern (view): everything the user sees in the Communication screen.
 * It prints the menu, the message log and messages and reads what the user
 * types, but makes no decisions - the {@link CommunicationController} does that.
 * Results start with {@code >>} and errors with {@code !!}, like the
 * Volunteers screen.
 */
public class CommunicationView {
    private final Scanner in;
    private final PrintStream out;

    /** The main menu passes its own Scanner, so all screens read the same input. */
    public CommunicationView(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    // ---- Menu and input ----

    /**
     * Shows the Communication menu and returns the number chosen,
     * -1 if the input was not a number, or 0 if there is no more input.
     *
     * @param zeroLabel "Exit" for a stand-alone screen, "Back" when opened from the main menu
     */
    public int showMenu(String zeroLabel) {
        out.println();
        out.println("========== COMMUNICATION ==========");
        out.println(" 1. Broadcast to a group");
        out.println(" 2. Send a message to one person");
        out.println(" 3. Message the attendees of one event");
        out.println(" 4. Show the message log");
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

    /** The message log, one line per message sent, or a note when nothing has been sent. */
    public void showLog(List<SentMessage> log) {
        if (log.isEmpty()) {
            out.println("No messages sent yet.");
            return;
        }
        for (SentMessage message : log) {
            out.println("  " + message);
        }
    }

    public void showMessage(String message) {
        out.println(">> " + message);
    }

    /** What the channels delivered (or skipped), one line each, already indented. */
    public void showDeliveries(List<String> lines) {
        for (String line : lines) {
            out.println(line);
        }
    }

    public void showError(String message) {
        out.println("!! " + message);
    }
}
