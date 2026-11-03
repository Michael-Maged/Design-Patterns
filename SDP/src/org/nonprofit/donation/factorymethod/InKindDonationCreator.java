package org.nonprofit.donation.factorymethod;

import java.time.LocalDate;
import org.nonprofit.donation.model.DonationForm;
import org.nonprofit.donation.model.DonationType;
import org.nonprofit.donation.model.Donor;

/** FACTORY METHOD (concrete creator): builds InKindDonation objects (goods, not money). */
public class InKindDonationCreator extends DonationCreator {

    @Override
    protected Donation createDonation(String id, Donor donor, LocalDate date, DonationForm form) {
        return new InKindDonation(id, donor, form.getAmount(), date, form.getItem());
    }

    @Override
    public DonationType getType() {
        return DonationType.IN_KIND;
    }
}
