package org.nonprofit.donation;

import java.time.LocalDate;

/** FACTORY METHOD pattern (concrete creator): builds {@link InKindDonation}s. */
public class InKindDonationProcessor extends DonationProcessor {

    @Override
    protected Donation createDonation(String id, Donor donor, LocalDate date, DonationDetails details) {
        return new InKindDonation(id, donor, details.amount(), date, details.campaign(), details.item());
    }

    @Override
    public DonationType getType() {
        return DonationType.IN_KIND;
    }
}
