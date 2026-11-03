package org.nonprofit.beneficiary;

import java.util.ArrayList;
import java.util.List;

import org.nonprofit.core.ContactPreference;
import org.nonprofit.core.Person;

/**
 * A person or household the organization supports. Keeps the needs they
 * have asked for help with and every distribution they have received.
 */
public class Beneficiary extends Person {
    private static int nextId = 1;

    private String area;
    private int householdSize;
    private final List<Need> needs = new ArrayList<>();
    private final List<Distribution> distributions = new ArrayList<>();

    public Beneficiary(String name, String email, String phone, ContactPreference preferredContact,
                       String area, int householdSize) {
        super(takeId(name, area, householdSize), name, email, phone, preferredContact);
        this.area = area.trim();
        this.householdSize = householdSize;
    }

    /** Beneficiaries often have only a phone, so they are reached by SMS. */
    public Beneficiary(String name, String phone, String area, int householdSize) {
        this(name, null, phone, ContactPreference.SMS, area, householdSize);
    }

    /** Checks every value before taking a number, so a rejected beneficiary does not use one up. */
    private static String takeId(String name, String area, int householdSize) {
        requireName(name);
        checkHousehold(area, householdSize);
        return "BEN-" + nextId++;
    }

    private static void checkHousehold(String area, int householdSize) {
        if (area == null || area.isBlank()) {
            throw new IllegalArgumentException("Area is required");
        }
        if (householdSize <= 0) {
            throw new IllegalArgumentException("Household size must be at least 1");
        }
    }

    // ---- Bookkeeping (driven by BeneficiaryService) ----

    /** Changes contact and household details. Everything is checked first, so a rejected update changes nothing. */
    void updateDetails(String name, String email, String phone, ContactPreference preferredContact,
                       String area, int householdSize) {
        requireName(name);
        checkHousehold(area, householdSize);
        updateContact(name, email, phone, preferredContact);
        this.area = area.trim();
        this.householdSize = householdSize;
    }

    void addNeed(Need need) {
        needs.add(need);
    }

    void removeNeed(Need need) {
        needs.remove(need);
    }

    void addDistribution(Distribution distribution) {
        distributions.add(distribution);
    }

    // ---- Queries ----

    public List<Need> openNeeds() {
        List<Need> open = new ArrayList<>();
        for (Need need : needs) {
            if (!need.isMet()) {
                open.add(need);
            }
        }
        return open;
    }

    public int unitsReceived() {
        int total = 0;
        for (Distribution d : distributions) {
            total += d.getQuantity();
        }
        return total;
    }

    // ---- Getters (copies, so callers cannot change the beneficiary's data) ----

    public String getArea() { return area; }
    public int getHouseholdSize() { return householdSize; }
    public List<Need> getNeeds() { return new ArrayList<>(needs); }
    public List<Distribution> getDistributions() { return new ArrayList<>(distributions); }

    public String summary() {
        return String.format("%s %s | %s, household of %d | %d need(s), %d open | %d unit(s) received",
                getId(), getName(), area, householdSize, needs.size(), openNeeds().size(), unitsReceived());
    }
}
