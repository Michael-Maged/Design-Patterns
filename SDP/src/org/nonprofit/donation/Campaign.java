package org.nonprofit.donation;

/** A fundraising campaign with a money goal; donations can count towards it. */
public class Campaign {
    private final String name;
    private final double goal;
    private double raised;

    public Campaign(String name, double goal) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Campaign name is required");
        }
        if (Double.isNaN(goal) || goal <= 0) {
            throw new IllegalArgumentException("Campaign goal must be a positive number");
        }
        this.name = name;
        this.goal = goal;
    }

    void addDonation(Donation donation) {
        raised += donation.getValue();
    }

    public String getName() { return name; }
    public double getGoal() { return goal; }
    public double getRaised() { return raised; }

    public double progressPercent() {
        return raised * 100 / goal;
    }

    public String summary() {
        return String.format("%s: %s of %s (%.0f%%)", name, DonationDocument.money(raised), DonationDocument.money(goal), progressPercent());
    }
}
