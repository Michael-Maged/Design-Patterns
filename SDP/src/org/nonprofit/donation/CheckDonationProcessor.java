package org.nonprofit.donation;

import java.time.LocalDate;

/** FACTORY METHOD pattern (concrete creator): builds {@link CheckDonation}s. */
public class CheckDonationProcessor extends DonationProcessor {

    @Override
    protected Donation createDonation(String id, Donor donor, LocalDate date, DonationDetails details) {
        return new CheckDonation(id, donor, details.amount(), date, details.campaign(),
                details.reference(), details.bank());
    }

    @Override
    public DonationType getType() {
        return DonationType.CHECK;
    }
}
