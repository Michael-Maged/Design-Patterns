package org.nonprofit.donation.templatemethod;

import java.time.LocalDate;
import java.util.Locale;

/**
 * TEMPLATE METHOD pattern.
 *
 * Every document we give to donors (a receipt, a tax statement) has the same
 * layout:  header -> body -> totals -> footer.
 * generate() is the "template method": it fixes that order once, here.
 * Each subclass only fills in the parts that are different (title, body, totals).
 *
 * generate() is final so a subclass can't change the order by mistake.
 */
public abstract class DonationDocument {
    public static final String ORGANIZATION = "Community Hands Foundation";
    private static final String LINE = "=".repeat(55);
    private static final String THIN_LINE = "-".repeat(55);

    private final LocalDate issueDate;

    protected DonationDocument(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    /** The template method: the steps always run in this order. */
    public final String generate() {
        if (!canBeIssued()) {
            throw new IllegalStateException("Nothing to show on the " + getTitle());
        }
        StringBuilder text = new StringBuilder();
        text.append(LINE).append("\n");
        text.append(header()).append("\n");
        text.append(THIN_LINE).append("\n");
        text.append(body()).append("\n");
        text.append(THIN_LINE).append("\n");
        text.append(totals()).append("\n");
        text.append(THIN_LINE).append("\n");
        text.append(footer()).append("\n");
        text.append(LINE);
        return text.toString();
    }

    // ---- Steps every subclass MUST write ----

    protected abstract String getTitle();

    protected abstract String body();

    protected abstract String totals();

    // ---- Hooks: they have a default, a subclass CAN change them if it wants ----

    /** Return false when there is nothing to print (e.g. no donations that year). */
    protected boolean canBeIssued() {
        return true;
    }

    protected String header() {
        return ORGANIZATION + "\n" + getTitle().toUpperCase(Locale.ROOT) + "\nIssued on: " + issueDate;
    }

    protected String footer() {
        return "Thank you for supporting our work!\n"
                + "No goods or services were given in exchange for this donation.";
    }
}
