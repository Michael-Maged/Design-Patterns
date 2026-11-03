package org.nonprofit.donation.templatemethod;

import java.time.LocalDate;
import java.util.List;
import org.nonprofit.donation.factorymethod.Donation;
import org.nonprofit.donation.model.DonationType;
import org.nonprofit.donation.model.Donor;

/**
 * TEMPLATE METHOD (concrete class): a yearly statement that lists all the
 * donations one donor made in a tax year, so they can use it for their taxes.
 */
public class TaxStatement extends DonationDocument {
    private final Donor donor;
    private final int year;

    public TaxStatement(Donor donor, int year, LocalDate issueDate) {
        super(issueDate);
        this.donor = donor;
        this.year = year;
    }

    @Override
    protected String getTitle() {
        return "Tax Statement " + year;
    }

    @Override
    protected String body() {
        StringBuilder text = new StringBuilder();
        text.append("Donor: ").append(donor.getName()).append(" (").append(donor.getId()).append(")\n\n");
        text.append(String.format("%-12s %-8s %-8s %12s", "Date", "ID", "Method", "Value"));
        for (Donation donation : donations()) {
            text.append("\n");
            text.append(String.format("%-12s %-8s %-8s %12s", donation.getDate(), donation.getId(),
                    donation.getType(), Donation.money(donation.getAmount())));
        }
        return text.toString();
    }

    /** Money and goods are shown apart, because goods are only an estimate. */
    @Override
    protected String totals() {
        double money = 0;
        double goods = 0;
        for (Donation donation : donations()) {
            if (donation.getType() == DonationType.IN_KIND) {
                goods += donation.getAmount();
            } else {
                money += donation.getAmount();
            }
        }
        return String.format("%-25s %12s%n%-25s %12s%n%-25s %12s",
                "Money donations:", Donation.money(money),
                "Goods (estimated):", Donation.money(goods),
                "Total for " + year + ":", Donation.money(money + goods));
    }

    /** Hook: no statement is printed for a year with no donations. */
    @Override
    protected boolean canBeIssued() {
        return !donations().isEmpty();
    }

    private List<Donation> donations() {
        return donor.getDonationsIn(year);
    }
}
