package org.nonprofit.donation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.Person;

/** A person or organization that gives to the non-profit, with their donation history. */
public class Donor extends Person {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final List<Donation> donations = new ArrayList<>();

    public Donor(String name, String email, String phone, ContactPreference preferredContact) {
        super("DNR-" + NEXT_ID.getAndIncrement(), name, email, phone, preferredContact);
    }

    /** Donors are thanked by e-mail unless they ask otherwise. */
    public Donor(String name, String email, String phone) {
        this(name, email, phone, ContactPreference.EMAIL);
    }

    void addDonation(Donation donation) {
        donations.add(donation);
    }

    public List<Donation> getDonations() {
        return List.copyOf(donations);
    }

    /** Donations made in one calendar year, oldest first. */
    public List<Donation> donationsIn(int year) {
        return donations.stream()
                .filter(d -> d.getDate().getYear() == year)
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .toList();
    }

    public double getTotalGiven() {
        return donations.stream().mapToDouble(Donation::getValue).sum();
    }

    public String summary() {
        return String.format("%s %s | %d donation(s), %s in total",
                getId(), getName(), donations.size(), DonationDocument.money(getTotalGiven()));
    }
}
