package org.nonprofit.donation.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.Person;
import org.nonprofit.donation.factorymethod.Donation;

/**
 * A person or a company that gives to the organization.
 * Besides the normal contact info (from Person) a donor keeps a history
 * of every donation they made, which we need for the tax statement.
 */
public class Donor extends Person {
    private final List<Donation> donations = new ArrayList<>();

    public Donor(String id, String name, String email, String phone, ContactPreference preferredContact) {
        super(id, name, email, phone, preferredContact);
    }

    /**
     * Records a donation in this donor's history. Called only by
     * DonationCreator.receive(); public because the creator lives in the
     * factorymethod package.
     */
    public void addDonation(Donation donation) {
        donations.add(donation);
    }

    /** Returns a copy so nobody outside can change the donor's history. */
    public List<Donation> getDonations() {
        return new ArrayList<>(donations);
    }

    /** All donations made in one year, oldest first (used by the tax statement). */
    public List<Donation> getDonationsIn(int year) {
        List<Donation> result = new ArrayList<>();
        for (Donation donation : donations) {
            if (donation.getDate().getYear() == year) {
                result.add(donation);
            }
        }
        result.sort(Comparator.comparing(Donation::getDate));
        return result;
    }

    public double getTotalGiven() {
        double total = 0;
        for (Donation donation : donations) {
            total += donation.getAmount();
        }
        return total;
    }

    public String summary() {
        return getId() + " | " + getName() + " | " + donations.size() + " donation(s) | "
                + Donation.money(getTotalGiven()) + " in total";
    }
}
