package org.nonprofit.volunteer;

import java.time.LocalDate;

/**
 * TEMPLATE METHOD pattern (abstract class): {@link #issue()} fixes the
 * layout of every certificate - title, recipient, achievement, signature -
 * and subclasses fill in only the parts that differ.
 */
public abstract class Certificate {
    private static final String BORDER = "+" + "-".repeat(58) + "+";

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
        line(sb, title().toUpperCase());
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

    private static void line(StringBuilder sb, String text) {
        sb.append(String.format("| %-56s |%n", text));
    }
}
