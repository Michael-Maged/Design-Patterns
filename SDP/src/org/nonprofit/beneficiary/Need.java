package org.nonprofit.beneficiary;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Something a beneficiary needs: a resource (food parcels, blankets) or a
 * service (counselling sessions, tutoring), counted in units. The category
 * is stored lower-case so "Food Parcel" matches "food parcel"; it is plain
 * text so it can later come from a look-up table.
 */
public class Need {
    private static int nextId = 1;

    private final String id;
    private final Beneficiary beneficiary;
    private final String category;
    private final int quantity;
    private final Urgency urgency;
    private final LocalDate requestedOn;
    private int received;

    Need(Beneficiary beneficiary, String category, int quantity, Urgency urgency, LocalDate requestedOn) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Need category is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        if (urgency == null || requestedOn == null) {
            throw new IllegalArgumentException("Urgency and request date are required");
        }
        this.id = "NED-" + nextId++;
        this.beneficiary = beneficiary;
        // Locale.ROOT so matching does not change with the computer's language (e.g. Turkish "I").
        this.category = category.trim().toLowerCase(Locale.ROOT);
        this.quantity = quantity;
        this.urgency = urgency;
        this.requestedOn = requestedOn;
    }

    /** Records units handed over. Giving more than is still needed is refused. */
    void receive(int units) {
        if (units <= 0) {
            throw new IllegalArgumentException("Units given must be at least 1");
        }
        if (units > remaining()) {
            throw new IllegalArgumentException(id + " only needs " + remaining() + " more " + category);
        }
        received += units;
    }

    public int remaining() {
        return quantity - received;
    }

    public boolean isMet() {
        return received >= quantity;
    }

    public double percentMet() {
        return received * 100.0 / quantity;
    }

    public String getId() { return id; }
    public Beneficiary getBeneficiary() { return beneficiary; }
    public String getCategory() { return category; }
    public int getQuantity() { return quantity; }
    public Urgency getUrgency() { return urgency; }
    public LocalDate getRequestedOn() { return requestedOn; }
    public int getReceived() { return received; }

    public String summary() {
        return String.format("%s %-20s %-6s %3d/%-3d requested %s by %s",
                id, category, urgency, received, quantity, requestedOn, beneficiary.getName());
    }

    @Override
    public String toString() {
        return id + " (" + category + ")";
    }
}
