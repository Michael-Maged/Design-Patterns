package org.nonprofit.event;

import java.time.LocalDateTime;

/** A fundraising event with a money goal and pledges collected on the day. */
public class Fundraiser extends Event {
    static final double DEFAULT_TICKET_PRICE = 100.0;

    private double fundraisingGoal;
    private double amountRaised;

    Fundraiser(String id, String name, LocalDateTime dateTime, String location, int capacity) {
        super(id, name, dateTime, location, capacity, DEFAULT_TICKET_PRICE);
    }

    @Override
    public EventType getType() {
        return EventType.FUNDRAISER;
    }

    public void setFundraisingGoal(double goal) {
        if (goal < 0) {
            throw new IllegalArgumentException("Goal cannot be negative");
        }
        this.fundraisingGoal = goal;
    }

    public void recordPledge(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Pledge must be positive");
        }
        amountRaised += amount;
    }

    public double getFundraisingGoal() { return fundraisingGoal; }
    public double getAmountRaised() { return amountRaised; }

    public double progressPercent() {
        return fundraisingGoal == 0 ? 0 : amountRaised * 100 / fundraisingGoal;
    }

    @Override
    protected String details() {
        return String.format("goal $%.2f, raised $%.2f (%.0f%%)", fundraisingGoal, amountRaised, progressPercent());
    }
}
