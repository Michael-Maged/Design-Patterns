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
        if (Double.isNaN(goal) || Double.isInfinite(goal) || goal < 0) {
            throw new IllegalArgumentException("Goal must be a non-negative number");
        }
        requireNotCancelled("change its goal");
        this.fundraisingGoal = goal;
    }

    public void recordPledge(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Pledge must be a positive number");
        }
        requireNotCancelled("record a pledge");
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
