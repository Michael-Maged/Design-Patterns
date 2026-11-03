package org.nonprofit.donation;

/**
 * The values a user enters for a new donation. Which ones are used depends
 * on the type: online uses the reference and the recurring flag, check uses
 * the reference (check number) and the bank, in-kind uses the item.
 * Built fluently, e.g. {@code DonationDetails.of(250).reference("PAY-1").recurring()}.
 */
public final class DonationDetails {
    private final double amount;
    private String reference;
    private String bank;
    private String item;
    private boolean recurring;
    private String campaign;

    private DonationDetails(double amount) {
        this.amount = amount;
    }

    /** @param amount money given, or estimated value for goods */
    public static DonationDetails of(double amount) {
        return new DonationDetails(amount);
    }

    /** Payment reference for online donations, check number for checks. */
    public DonationDetails reference(String reference) {
        this.reference = reference;
        return this;
    }

    public DonationDetails bank(String bank) {
        this.bank = bank;
        return this;
    }

    public DonationDetails item(String item) {
        this.item = item;
        return this;
    }

    public DonationDetails recurring() {
        this.recurring = true;
        return this;
    }

    public DonationDetails campaign(String campaign) {
        this.campaign = campaign;
        return this;
    }

    double amount() { return amount; }
    String reference() { return reference; }
    String bank() { return bank; }
    String item() { return item; }
    boolean isRecurring() { return recurring; }
    String campaign() { return campaign; }
}
