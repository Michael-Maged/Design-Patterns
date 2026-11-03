package org.nonprofit.donation.model;

import org.nonprofit.donation.factorymethod.Donation;

/**
 * A fundraising campaign with a money goal, for example "Winter Blankets, goal $15,000".
 * Donations can be linked to a campaign so we can see how close we are to the goal.
 * (This was one of the extra requirements we collected from the client.)
 */
public class Campaign {
    private final String name;
    private final double goal;
    private double raised;

    public Campaign(String name, double goal) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Campaign name is required");
        }
        if (!Double.isFinite(goal) || goal <= 0) {
            throw new IllegalArgumentException("Campaign goal must be more than zero");
        }
        this.name = name.trim();
        this.goal = goal;
    }

    void addAmount(double amount) {
        raised += amount;
    }

    public String getName() { return name; }
    public double getGoal() { return goal; }
    public double getRaised() { return raised; }

    /** How much of the goal is reached, in percent. Can go above 100. */
    public double getProgress() {
        return raised * 100 / goal;
    }

    public boolean isGoalReached() {
        return raised >= goal;
    }

    @Override
    public String toString() {
        String text = String.format("%s: %s of %s (%.0f%%)", name, Donation.money(raised), Donation.money(goal), getProgress());
        if (isGoalReached()) {
            text += " - goal reached!";
        }
        return text;
    }
}
