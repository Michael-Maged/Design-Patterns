package org.nonprofit.donation;

import java.time.LocalDate;

/**
 * What every donation has in common: who gave it, how much, and when.
 * For an in-kind donation (goods) the amount is the estimated value of the goods.
 *
 * The class is abstract because a donation is always one of the concrete
 * types (online, check or in-kind). Those objects are built by DonationFactory.
 */
public abstract class Donation {
    private final String id;
    private final Donor donor;
    private final double amount;
    private final LocalDate date;
    private Campaign campaign; // null when the donation is not for a campaign

    protected Donation(String id, Donor donor, double amount, LocalDate date) {
        if (donor == null) {
            throw new IllegalArgumentException("Donor is required");
        }
        // Double.isNaN is needed because NaN is not caught by "amount <= 0".
        if (Double.isNaN(amount) || amount <= 0) {
            throw new IllegalArgumentException("Donation amount must be more than zero");
        }
        if (date == null) {
            throw new IllegalArgumentException("Donation date is required");
        }
        this.id = id;
        this.donor = donor;
        this.amount = amount;
        this.date = date;
    }

    /** Each subclass says which type it is. */
    public abstract DonationType getType();

    /** Each subclass describes its own extra info (payment ref, check number, item...). */
    public abstract String getDetails();

    public String getId() { return id; }
    public Donor getDonor() { return donor; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public Campaign getCampaign() { return campaign; }

    void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    /** Formats money the same way everywhere in the module, e.g. $1,250.00 */
    public static String money(double amount) {
        return String.format("$%,.2f", amount);
    }

    @Override
    public String toString() {
        String text = id + " | " + getType() + " | " + donor.getName() + " | " + money(amount)
                + " | " + date + " | " + getDetails();
        if (campaign != null) {
            text += " | campaign: " + campaign.getName();
        }
        return text;
    }
}
