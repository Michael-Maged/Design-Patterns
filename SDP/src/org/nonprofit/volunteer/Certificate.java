package org.nonprofit.volunteer;

import java.time.LocalDate;
import java.util.Locale;

/**
 * TEMPLATE METHOD pattern (abstract class): {@link #issue()} fixes the
 * layout of every certificate - title, recipient, achievement, signature -
 * and subclasses fill in only the parts that differ.
 */
public abstract class Certificate {
    private static final int WIDTH = 56;
    private static final String BORDER = "+" + "-".repeat(WIDTH + 2) + "+";

    protected final Volunteer volunteer;
    protected final LocalDate issueDate;

    protected Certificate(Volunteer volunteer, LocalDate issueDate) {
        this.volunteer = volunteer;
        this.issueDate = issueDate;
    }

    /** The template method. Final so subclasses cannot change the order of the steps. */
    public final String issue() {
        if (!isEligible()) {
            throw new IllegalStateException(volunteer.getName() + " is not eligible for a " + title());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(BORDER).append('\n');
        line(sb, title().toUpperCase(Locale.ROOT));
        line(sb, "");
        line(sb, recipient());
        line(sb, "");
        for (String text : achievement().split("\\R")) {
            line(sb, text);
        }
        line(sb, "");
        line(sb, signature());
        sb.append(BORDER);
        return sb.toString();
    }

    // ---- Steps that every subclass must provide ----

    protected abstract String title();

    protected abstract String achievement();

    // ---- Hooks with a default that subclasses may override ----

    protected boolean isEligible() {
        return true;
    }

    protected String recipient() {
        return "Presented to " + volunteer.getName() + " (" + volunteer.getId() + ")";
    }

    protected String signature() {
        return "Issued " + issueDate + " - Volunteer Coordinator";
    }

    /** Writes one row, wrapping long text onto extra rows so it never passes the border. */
    private static void line(StringBuilder sb, String text) {
        String rest = text;
        do {
            int cut = rest.length() <= WIDTH ? rest.length() : breakPoint(rest);
            sb.append(String.format("| %-" + WIDTH + "s |%n", rest.substring(0, cut)));
            rest = rest.substring(cut).stripLeading();
        } while (!rest.isEmpty());
    }

    /** Last space that fits on the row, or a hard cut if a single word is too long. */
    private static int breakPoint(String text) {
        int space = text.lastIndexOf(' ', WIDTH);
        return space > 0 ? space : WIDTH;
    }
}
