package org.nonprofit.donation;

import java.time.LocalDate;

/** FACTORY METHOD pattern (concrete creator): builds {@link OnlineDonation}s. */
public class OnlineDonationProcessor extends DonationProcessor {

    @Override
    protected Donation createDonation(String id, Donor donor, LocalDate date, DonationDetails details) {
        return new OnlineDonation(id, donor, details.amount(), date, details.campaign(),
                details.reference(), details.isRecurring());
    }

    @Override
    public DonationType getType() {
        return DonationType.ONLINE;
    }
}
