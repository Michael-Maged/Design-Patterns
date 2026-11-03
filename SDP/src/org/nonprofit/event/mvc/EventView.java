package org.nonprofit.event.mvc;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;
import org.nonprofit.event.simplefactory.Fundraiser;
import org.nonprofit.event.simplefactory.Workshop;

/**
 * MVC pattern (view): everything the user sees in the Events screen.
 * It prints menus, lists and messages and reads what the user types, but
 * makes no decisions - the {@link EventController} does that.
 * Results start with {@code >>} and errors with {@code !!}, like the
 * Volunteers screen.
 */
public class EventView {
    private final Scanner in;
    private final PrintStream out;

    /** The main menu passes its own Scanner, so all screens read the same input. */
    public EventView(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    // ---- Menus ----

    /** @param zeroLabel "Exit" for a stand-alone screen, "Back" when opened from the main menu */
    public int showMenu(String zeroLabel) {
        return showMenu("EVENTS", zeroLabel,
                "List events", "List upcoming events (by date)", "Create an event",
                "Edit an event (name, date, place, seats, price, type details)",
                "Register an attendee", "Cancel a registration / leave the waitlist", "Check in an attendee",
                "Update an attendee's contact details", "Send a reminder for one event",
                "Send reminders (next 24 hours)", "Attendance report", "Cancel an event", "Delete an event");
    }

    /** The changes "Edit an event" offers, for the event named in {@code title}. */
    public int showEditMenu(String title) {
        return showMenu("EDIT " + title, "Back",
                "Rename", "Reschedule", "Relocate", "Change capacity", "Change base ticket price",
                "Fundraiser: set the goal", "Fundraiser: record a pledge",
                "Workshop: topic and instructor", "Outreach: target community");
    }

    /** Numbered options from 1, then option 0 ("Back" or "Exit"). */
    private int showMenu(String title, String zeroLabel, String... options) {
        out.println();
        out.println("========== " + title + " ==========");
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

    /** For multi-line text such as the attendance report. */
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
