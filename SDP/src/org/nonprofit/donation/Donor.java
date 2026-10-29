package org.nonprofit.donation;

import java.util.ArrayList;
import java.util.List;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.Person;

/** A person or organization that gives to the non-profit, with their donation history. */
public class Donor extends Person {
    private static int nextId = 1;

    private final List<Donation> donations = new ArrayList<>();

    public Donor(String name, String email, String phone, ContactPreference preferredContact) {
        super("DNR-" + nextId++, name, email, phone, preferredContact);
    }

    /** Donors are thanked by e-mail unless they ask otherwise. */
    public Donor(String name, String email, String phone) {
        this(name, email, phone, ContactPreference.EMAIL);
    }

    void addDonation(Donation donation) {
        donations.add(donation);
    }

    /** A copy, so callers cannot change the donor's history. */
    public List<Donation> getDonations() {
        return new ArrayList<>(donations);
    }

    /** Donations made in one calendar year, oldest first. */
    public List<Donation> donationsIn(int year) {
        List<Donation> result = new ArrayList<>();
        for (Donation d : donations) {
            if (d.getDate().getYear() != year) {
                continue;
            }
            // Insert before the first donation with a later date.
            int position = result.size();
            for (int i = 0; i < result.size(); i++) {
                if (result.get(i).getDate().isAfter(d.getDate())) {
                    position = i;
                    break;
                }
            }
            result.add(position, d);
        }
        return result;
    }

    public double getTotalGiven() {
        double total = 0;
        for (Donation d : donations) {
            total += d.getValue();
        }
        return total;
    }

    public String summary() {
        return String.format("%s %s | %d donation(s), %s in total",
                getId(), getName(), donations.size(), DonationDocument.money(getTotalGiven()));
    }
}
