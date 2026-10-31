package org.nonprofit.donation;

import java.time.LocalDate;

/**
 * Base class for all donations. {@code value} is the money given, or the
 * estimated value of goods for an in-kind donation.
 *
 * FACTORY METHOD pattern (product). Concrete types are created only by the
 * {@link DonationProcessor} subclasses.
 */
public abstract class Donation {
    private final String id;
    private final Donor donor;
    private final double value;
    private final LocalDate date;
    private final String campaign;

    protected Donation(String id, Donor donor, double value, LocalDate date, String campaign) {
        if (donor == null) {
            throw new IllegalArgumentException("Donor is required");
        }
        // NaN ("not a number") would slip past "value <= 0", so it is checked too.
        if (Double.isNaN(value) || value <= 0) {
            throw new IllegalArgumentException("Donation amount must be a positive number");
        }
        if (date == null) {
            throw new IllegalArgumentException("Donation date is required");
        }
        this.id = id;
        this.donor = donor;
        this.value = value;
        this.date = date;
        this.campaign = campaign;
    }

    public abstract DonationType getType();

    /** Type-specific information shown on summaries and receipts. */
    public abstract String details();

    public String getId() { return id; }
    public Donor getDonor() { return donor; }
    public double getValue() { return value; }
    public LocalDate getDate() { return date; }
    /** The campaign this donation counts towards, or null. */
    public String getCampaign() { return campaign; }

    public String summary() {
        return String.format("%s [%s] %s | %s on %s | %s%s",
                id, getType(), donor.getName(), DonationDocument.money(value), date, details(),
                campaign == null ? "" : " | campaign: " + campaign);
    }

    @Override
    public String toString() {
        return getType() + " donation " + id;
    }
}
