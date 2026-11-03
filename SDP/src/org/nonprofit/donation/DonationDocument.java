package org.nonprofit.donation;

import java.time.LocalDate;
import java.util.Locale;

/**
 * TEMPLATE METHOD pattern (abstract class): {@link #generate()} fixes the
 * layout of every donation document - header, body, totals, footer - and
 * subclasses fill in only the body and the totals.
 */
public abstract class DonationDocument {
    static final String ORGANIZATION = "Community Hands Foundation";
    private static final String RULE = "=".repeat(60);
    private static final String THIN_RULE = "-".repeat(60);

    protected final LocalDate issueDate;

    protected DonationDocument(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    /** The template method. Final so subclasses cannot change the order of the steps. */
    public final String generate() {
        if (!hasContent()) {
            throw new IllegalStateException("Nothing to include in the " + title());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(RULE).append('\n');
        sb.append(header()).append('\n');
        sb.append(THIN_RULE).append('\n');
        sb.append(body()).append('\n');
        sb.append(THIN_RULE).append('\n');
        sb.append(totals()).append('\n');
        sb.append(THIN_RULE).append('\n');
        sb.append(footer()).append('\n');
        sb.append(RULE);
        return sb.toString();
    }

    // ---- Steps that every subclass must provide ----

    protected abstract String title();

    protected abstract String body();

    protected abstract String totals();

    // ---- Hooks with a default that subclasses may override ----

    protected boolean hasContent() {
        return true;
    }

    protected String header() {
        return String.format("%s%n%s%nIssued: %s", ORGANIZATION, title().toUpperCase(Locale.ROOT), issueDate);
    }

    protected String footer() {
        return "Thank you for your support.\n"
                + "No goods or services were provided in exchange for this gift.";
    }

    static String money(double amount) {
        return String.format("$%,.2f", amount);
    }
}
