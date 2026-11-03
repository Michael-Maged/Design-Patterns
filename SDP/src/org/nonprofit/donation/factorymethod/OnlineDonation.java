package org.nonprofit.donation.factorymethod;

import java.time.LocalDate;
import org.nonprofit.donation.model.DonationType;
import org.nonprofit.donation.model.Donor;

/**
 * Money paid online (card or bank transfer). The donor can choose to repeat it every month.
 * The constructor is package-private: only OnlineDonationCreator creates it.
 */
public class OnlineDonation extends Donation {
    private final String paymentReference;
    private final boolean monthly;

    OnlineDonation(String id, Donor donor, double amount, LocalDate date,
                   String paymentReference, boolean monthly) {
        super(id, donor, amount, date);
        if (paymentReference == null || paymentReference.isBlank()) {
            throw new IllegalArgumentException("Online donations need a payment reference");
        }
        this.paymentReference = paymentReference;
        this.monthly = monthly;
    }

    @Override
    public DonationType getType() {
        return DonationType.ONLINE;
    }

    @Override
    public String getDetails() {
        String text = "payment ref " + paymentReference;
        if (monthly) {
            text += ", every month";
        }
        return text;
    }

    public String getPaymentReference() { return paymentReference; }
    public boolean isMonthly() { return monthly; }
}
