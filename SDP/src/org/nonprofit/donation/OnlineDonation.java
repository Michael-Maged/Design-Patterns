package org.nonprofit.donation;

import java.time.LocalDate;

/** A card or bank-transfer donation made online, optionally repeated every month. */
public class OnlineDonation extends Donation {
    private final String paymentReference;
    private final boolean recurringMonthly;

    OnlineDonation(String id, Donor donor, double amount, LocalDate date, String campaign,
                   String paymentReference, boolean recurringMonthly) {
        super(id, donor, amount, date, campaign);
        if (paymentReference == null || paymentReference.isBlank()) {
            throw new IllegalArgumentException("Online donations need a payment reference");
        }
        this.paymentReference = paymentReference;
        this.recurringMonthly = recurringMonthly;
    }

    @Override
    public DonationType getType() {
        return DonationType.ONLINE;
    }

    public String getPaymentReference() { return paymentReference; }
    public boolean isRecurringMonthly() { return recurringMonthly; }

    @Override
    public String details() {
        return "payment ref " + paymentReference + (recurringMonthly ? ", recurring monthly" : "");
    }
}
