package org.nonprofit.donation.factorymethod;

import java.time.LocalDate;
import org.nonprofit.donation.model.DonationForm;
import org.nonprofit.donation.model.DonationType;
import org.nonprofit.donation.model.Donor;

/** FACTORY METHOD (concrete creator): builds OnlineDonation objects. */
public class OnlineDonationCreator extends DonationCreator {

    @Override
    protected Donation createDonation(String id, Donor donor, LocalDate date, DonationForm form) {
        return new OnlineDonation(id, donor, form.getAmount(), date, form.getReference(), form.isMonthly());
    }

    @Override
    public DonationType getType() {
        return DonationType.ONLINE;
    }
}
